package org.sciborgs1155.robot.drive;

import static edu.wpi.first.units.Units.Meters;
import static edu.wpi.first.units.Units.MetersPerSecond;
import static edu.wpi.first.units.Units.MetersPerSecondPerSecond;
import static edu.wpi.first.units.Units.Radians;
import static edu.wpi.first.units.Units.Seconds;
import static edu.wpi.first.units.Units.Volts;
import static java.lang.Math.atan;
import static org.sciborgs1155.lib.Assertion.*;
import static org.sciborgs1155.robot.Constants.allianceRotation;
import static org.sciborgs1155.robot.Ports.Drive.*;
import static org.sciborgs1155.robot.drive.DriveConstants.*;

import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.Vector;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.controller.ProfiledPIDController;
import edu.wpi.first.math.estimator.SwerveDrivePoseEstimator;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.math.numbers.N3;
import edu.wpi.first.math.trajectory.TrapezoidProfile;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.FieldObject2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import edu.wpi.first.wpilibj2.command.sysid.SysIdRoutine;
import edu.wpi.first.wpilibj2.command.sysid.SysIdRoutine.Direction;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import monologue.Annotations.IgnoreLogged;
import monologue.Annotations.Log;
import monologue.Logged;
import org.photonvision.EstimatedRobotPose;
import org.sciborgs1155.lib.Assertion;
import org.sciborgs1155.lib.InputStream;
import org.sciborgs1155.lib.Test;
import org.sciborgs1155.robot.Constants;
import org.sciborgs1155.robot.Robot;
import org.sciborgs1155.robot.drive.DriveConstants.ControlMode;
import org.sciborgs1155.robot.drive.DriveConstants.Rotation;
import org.sciborgs1155.robot.drive.DriveConstants.Translation;
import org.sciborgs1155.robot.vision.Vision.PoseEstimate;

public class Drive extends SubsystemBase implements Logged, AutoCloseable {
  // Modules
  private final ModuleIO frontLeft;
  private final ModuleIO frontRight;
  private final ModuleIO rearLeft;
  private final ModuleIO rearRight;

  @IgnoreLogged private final List<ModuleIO> modules;

  // Gyro, navX2-MXP
  private final GyroIO gyro;
  private static Rotation2d simRotation = new Rotation2d();

  public final SwerveDriveKinematics kinematics = new SwerveDriveKinematics(MODULE_OFFSET);

  // Odometry and pose estimation
  private final SwerveDrivePoseEstimator odometry;

  @Log.NT private final Field2d field2d = new Field2d();
  private final FieldObject2d[] modules2d;

  // Characterization routines
  private final SysIdRoutine translationCharacterization;
  private final SysIdRoutine rotationalCharacterization;

  // Movement automation
  @Log.NT
  private final ProfiledPIDController translationController =
      new ProfiledPIDController(
          Translation.P,
          Translation.I,
          Translation.D,
          new TrapezoidProfile.Constraints(
              MAX_SPEED.in(MetersPerSecond), MAX_ACCEL.in(MetersPerSecondPerSecond)));

  @Log.NT
  private final PIDController rotationController =
      new PIDController(Rotation.P, Rotation.I, Rotation.D);

  /**
   * A factory to create a new swerve drive based on the type of module used / real or simulation.
   */
  public static Drive create() {
    if (Robot.isReal()) {
      return switch (TYPE) {
        case TALON ->
            new Drive(
                new NavXGyro(),
                new TalonModule(FRONT_LEFT_DRIVE, FRONT_LEFT_TURNING, ANGULAR_OFFSETS.get(0), "FL"),
                new TalonModule(
                    FRONT_RIGHT_DRIVE, FRONT_RIGHT_TURNING, ANGULAR_OFFSETS.get(1), "FR"),
                new TalonModule(REAR_LEFT_DRIVE, REAR_LEFT_TURNING, ANGULAR_OFFSETS.get(2), "RL"),
                new TalonModule(
                    REAR_RIGHT_DRIVE, REAR_RIGHT_TURNING, ANGULAR_OFFSETS.get(3), "RR"));
        case SPARK ->
            new Drive(
                new NavXGyro(),
                new SparkModule(FRONT_LEFT_DRIVE, FRONT_LEFT_TURNING, ANGULAR_OFFSETS.get(0), "FL"),
                new SparkModule(
                    FRONT_RIGHT_DRIVE, FRONT_RIGHT_TURNING, ANGULAR_OFFSETS.get(1), "FR"),
                new SparkModule(REAR_LEFT_DRIVE, REAR_LEFT_TURNING, ANGULAR_OFFSETS.get(2), "RL"),
                new SparkModule(
                    REAR_RIGHT_DRIVE, REAR_RIGHT_TURNING, ANGULAR_OFFSETS.get(3), "RR"));
      };
    } else {
      return new Drive(
          new NoGyro(),
          new SimModule("FL"),
          new SimModule("FR"),
          new SimModule("RL"),
          new SimModule("RR"));
    }
  }

  /** A factory to create a nonexistent swerve drive. */
  public static Drive none() {
    return new Drive(new NoGyro(), new NoModule(), new NoModule(), new NoModule(), new NoModule());
  }

  /** A swerve drive subsystem containing four {@link ModuleIO} modules and a gyroscope. */
  public Drive(
      GyroIO gyro, ModuleIO frontLeft, ModuleIO frontRight, ModuleIO rearLeft, ModuleIO rearRight) {
    this.gyro = gyro;
    this.frontLeft = frontLeft;
    this.frontRight = frontRight;
    this.rearLeft = rearLeft;
    this.rearRight = rearRight;

    modules = List.of(this.frontLeft, this.frontRight, this.rearLeft, this.rearRight);
    modules2d = new FieldObject2d[modules.size()];

    translationCharacterization =
        new SysIdRoutine(
            new SysIdRoutine.Config(),
            new SysIdRoutine.Mechanism(
                volts ->
                    modules.forEach(
                        m -> m.updateInputs(Rotation2d.fromRadians(0), volts.in(Volts))),
                null,
                this,
                "translation"));
    rotationalCharacterization =
        new SysIdRoutine(
            new SysIdRoutine.Config(),
            new SysIdRoutine.Mechanism(
                volts -> {
                  this.frontLeft.updateInputs(
                      Rotation2d.fromRadians(3 * Math.PI / 4), volts.in(Volts));
                  this.frontRight.updateInputs(
                      Rotation2d.fromRadians(Math.PI / 4), volts.in(Volts));
                  this.rearLeft.updateInputs(
                      Rotation2d.fromRadians(-3 * Math.PI / 4), volts.in(Volts));
                  this.rearRight.updateInputs(
                      Rotation2d.fromRadians(-Math.PI / 4), volts.in(Volts));
                },
                null,
                this,
                "rotation"));

    odometry =
        new SwerveDrivePoseEstimator(
            kinematics,
            gyro.rotation2d(),
            modulePositions(),
            new Pose2d(new Translation2d(), Rotation2d.fromDegrees(180)));

    for (int i = 0; i < modules.size(); i++) {
      var module = modules.get(i);
      modules2d[i] = field2d.getObject("module-" + module.name());
    }

    gyro.reset();

    translationController.setTolerance(Translation.TOLERANCE.in(Meters));
    rotationController.enableContinuousInput(0, 2 * Math.PI);
    rotationController.setTolerance(Rotation.TOLERANCE.in(Radians));

    SmartDashboard.putData(
        "translation quasistatic forward",
        translationCharacterization.quasistatic(Direction.kForward));
    SmartDashboard.putData(
        "translation dynamic forward", translationCharacterization.dynamic(Direction.kForward));
    SmartDashboard.putData(
        "translation quasistatic backward",
        translationCharacterization.quasistatic(Direction.kReverse));
    SmartDashboard.putData(
        "translation dynamic backward", translationCharacterization.dynamic(Direction.kReverse));
    SmartDashboard.putData(
        "rotation quasistatic forward", rotationalCharacterization.quasistatic(Direction.kForward));
    SmartDashboard.putData(
        "rotation dynamic forward", rotationalCharacterization.dynamic(Direction.kForward));
    SmartDashboard.putData(
        "rotation quasistatic backward",
        rotationalCharacterization.quasistatic(Direction.kReverse));
    SmartDashboard.putData(
        "rotation dynamic backward", rotationalCharacterization.dynamic(Direction.kReverse));
  }

  /**
   * Returns the currently-estimated pose of the robot.
   *
   * @return The pose.
   */
  @Log.NT
  public Pose2d pose() {
    return odometry.getEstimatedPosition();
  }

  /**
   * Returns the currently-estimated field-relative yaw of the robot.
   *
   * @return The rotation.
   */
  public Rotation2d heading() {
    return pose().getRotation();
  }

  /**
   * Resets the odometry to the specified pose.
   *
   * @param pose The pose to which to set the odometry.
   */
  public void resetOdometry(Pose2d pose) {
    odometry.resetPosition(gyro.rotation2d(), modulePositions(), pose);
  }

  /**
   * Drives the robot based on a {@link InputStream} for field relative x y and omega velocities.
   *
   * @param vx A supplier for the velocity of the robot along the x axis (perpendicular to the
   *     alliance side).
   * @param vy A supplier for the velocity of the robot along the y axis (parallel to the alliance
   *     side).
   * @param vOmega A supplier for the angular velocity of the robot.
   * @return The driving command.
   */
  public Command drive(DoubleSupplier vx, DoubleSupplier vy, DoubleSupplier vOmega) {
    return run(
        () ->
            setChassisSpeeds(
                ChassisSpeeds.fromFieldRelativeSpeeds(
                    vx.getAsDouble(),
                    vy.getAsDouble(),
                    vOmega.getAsDouble(),
                    heading().plus(allianceRotation())),
                ControlMode.OPEN_LOOP_VELOCITY));
  }

  /**
   * Drives the robot based on a {@link InputStream} for field relative x y and omega velocities.
   *
   * @param vx A supplier for the velocity of the robot along the x axis (perpendicular to the
   *     alliance side).
   * @param vy A supplier for the velocity of the robot along the y axis (parallel to the alliance
   *     side).
   * @param heading A supplier for the field relative heading of the robot.
   * @return The driving command.
   */
  public Command drive(DoubleSupplier vx, DoubleSupplier vy, Supplier<Rotation2d> heading) {
    return drive(
            vx,
            vy,
            () -> rotationController.calculate(heading().getRadians(), heading.get().getRadians()))
        .beforeStarting(rotationController::reset);
  }

  /**
   * Drives the robot while facing a target pose.
   *
   * @param vx A supplier for the absolute x velocity of the robot.
   * @param vy A supplier for the absolute y velocity of the robot.
   * @param translation A supplier for the translation2d to face on the field.
   * @return A command to drive while facing a target.
   */
  public Command driveFacingTarget(
      DoubleSupplier vx, DoubleSupplier vy, Supplier<Translation2d> translation) {
    return drive(vx, vy, () -> translation.get().minus(pose().getTranslation()).getAngle());
  }

  @Log.NT
  public boolean atRotationalSetpoint() {
    return rotationController.atSetpoint();
  }

  /**
   * Checks whether the robot is facing towards a point on the field.
   *
   * @param target The field-relative point to check.
   * @return Whether the robot is facing the target closely enough.
   */
  public boolean isFacing(Translation2d target) {
    return Math.abs(
            gyro.rotation2d().getRadians()
                - target.minus(pose().getTranslation()).getAngle().getRadians())
        < rotationController.getErrorTolerance();
  }

  /**
   * Sets the states of each swerve module using target speeds that the drivetrain will work to
   * reach.
   *
   * @param speeds The speeds the drivetrain will run at.
   * @param mode The control loop used to achieve those speeds.
   */
  public void setChassisSpeeds(ChassisSpeeds speeds, ControlMode mode) {
    setModuleStates(
        kinematics.toSwerveModuleStates(
            ChassisSpeeds.discretize(speeds, Constants.PERIOD.in(Seconds))),
        mode);
  }

  /**
   * Sets the states of each of the swerve modules.
   *
   * @param desiredStates The desired SwerveModule states.
   * @param mode The method to use when controlling the drive motor.
   */
  public void setModuleStates(SwerveModuleState[] desiredStates, ControlMode mode) {
    if (desiredStates.length != modules.size()) {
      throw new IllegalArgumentException("desiredStates must have the same length as modules");
    }

    SwerveDriveKinematics.desaturateWheelSpeeds(desiredStates, MAX_SPEED.in(MetersPerSecond));

    for (int i = 0; i < modules.size(); i++) {
      modules.get(i).updateSetpoint(desiredStates[i], mode);
    }
  }

  /**
   * Command factory that automatically path-follows, in a straight line, to a position on the
   * field.
   *
   * @param target The pose to reach.
   * @return The command to run the control loop until the pose is reached.
   */
  public Command driveTo(Pose2d target) {
    return run(() -> {
          Transform2d transform = pose().minus(target);
          Vector<N3> difference =
              VecBuilder.fill(
                  transform.getX(),
                  transform.getY(),
                  transform.getRotation().getRadians() * RADIUS.in(Meters));
          double out = translationController.calculate(difference.norm(), 0);
          Vector<N3> velocities = difference.unit().times(out);
          setChassisSpeeds(
              new ChassisSpeeds(
                  velocities.get(0), velocities.get(1), velocities.get(2) / RADIUS.in(Meters)),
              ControlMode.CLOSED_LOOP_VELOCITY);
        })
        .until(translationController::atGoal)
        .withName("drive to pose");
  }

  /** Resets all drive encoders to read a position of 0. */
  public void resetEncoders() {
    modules.forEach(ModuleIO::resetEncoders);
  }

  /** Zeroes the heading of the robot. */
  public Command zeroHeading() {
    return runOnce(gyro::reset);
  }

  /** Returns the module states. */
  @Log.NT
  public SwerveModuleState[] moduleStates() {
    return modules.stream().map(ModuleIO::state).toArray(SwerveModuleState[]::new);
  }

  /** Returns the module states. */
  @Log.NT
  private SwerveModuleState[] moduleSetpoints() {
    return modules.stream().map(ModuleIO::desiredState).toArray(SwerveModuleState[]::new);
  }

  /** Returns the module positions. */
  @Log.NT
  public SwerveModulePosition[] modulePositions() {
    return modules.stream().map(ModuleIO::position).toArray(SwerveModulePosition[]::new);
  }

  /** Returns the robot-relative chassis speeds. */
  @Log.NT
  public ChassisSpeeds robotRelativeChassisSpeeds() {
    return kinematics.toChassisSpeeds(moduleStates());
  }

  /** Returns the field-relative chassis speeds. */
  @Log.NT
  public ChassisSpeeds fieldRelativeChassisSpeeds() {
    return ChassisSpeeds.fromRobotRelativeSpeeds(robotRelativeChassisSpeeds(), heading());
  }

  /**
   * Updates pose estimate based on vision-provided {@link EstimatedRobotPose}s.
   *
   * @param poses The pose estimates based on vision data.
   */
  public void updateEstimates(PoseEstimate... poses) {
    Pose3d[] loggedEstimates = new Pose3d[poses.length];
    for (int i = 0; i < poses.length; i++) {
      loggedEstimates[i] = poses[i].estimatedPose().estimatedPose;
      odometry.addVisionMeasurement(
          poses[i].estimatedPose().estimatedPose.toPose2d(),
          poses[i].estimatedPose().timestampSeconds,
          poses[i].standardDev());
      field2d
          .getObject("Cam " + i + " Est Pose")
          .setPose(poses[i].estimatedPose().estimatedPose.toPose2d());
    }
    log("estimated poses", loggedEstimates);
  }

  @Override
  public void periodic() {
    // update our heading in reality / sim
    odometry.update(Robot.isReal() ? gyro.rotation2d() : simRotation, modulePositions());

    // update our simulated field poses
    field2d.setRobotPose(pose());

    for (int i = 0; i < modules2d.length; i++) {
      var module = modules.get(i);
      var transform = new Transform2d(MODULE_OFFSET[i], module.position().angle);
      modules2d[i].setPose(pose().transformBy(transform));
    }

    log(
        "turning target",
        new Pose2d(pose().getTranslation(), new Rotation2d(rotationController.getSetpoint())));

    log("command", Optional.ofNullable(getCurrentCommand()).map(Command::getName).orElse("none"));
  }

  @Override
  public void simulationPeriodic() {
    simRotation =
        simRotation.rotateBy(
            Rotation2d.fromRadians(
                robotRelativeChassisSpeeds().omegaRadiansPerSecond * Constants.PERIOD.in(Seconds)));
  }

  /** Stops the drivetrain. */
  public Command stop() {
    return runOnce(() -> setChassisSpeeds(new ChassisSpeeds(), ControlMode.OPEN_LOOP_VELOCITY));
  }

  /** Sets the drivetrain to an "X" configuration, preventing movement. */
  public Command lock() {
    var front = new SwerveModuleState(0, Rotation2d.fromDegrees(45));
    var back = new SwerveModuleState(0, Rotation2d.fromDegrees(-45));
    return run(
        () ->
            setModuleStates(
                new SwerveModuleState[] {front, back, back, front},
                ControlMode.OPEN_LOOP_VELOCITY));
  }

  /**
   * Factory for our drive systems check.
   *
   * <p>Checks for properly functioning movement and speed / heading measurements.
   *
   * @return The test to run.
   */
  public Test systemsCheck() {
    ChassisSpeeds speeds = new ChassisSpeeds(1, 1, 0);
    Command testCommand =
        run(() -> setChassisSpeeds(speeds, ControlMode.OPEN_LOOP_VELOCITY)).withTimeout(0.75);
    Function<ModuleIO, TruthAssertion> speedCheck =
        m ->
            tAssert(
                () -> m.state().speedMetersPerSecond * Math.signum(m.position().angle.getCos()) > 1,
                "Drive Syst Check " + m.name() + " Module Speed",
                () -> "expected: >= 1; actual: " + m.state().speedMetersPerSecond);
    Function<ModuleIO, EqualityAssertion> atAngle =
        m ->
            eAssert(
                "Drive Syst Check " + m.name() + " Module Angle (degrees)",
                () -> 45,
                () -> Units.radiansToDegrees(atan(m.position().angle.getTan())),
                1);
    Set<Assertion> assertions =
        modules.stream()
            .flatMap(m -> Stream.of(speedCheck.apply(m), atAngle.apply(m)))
            .collect(Collectors.toSet());
    return new Test(testCommand, assertions);
  }

  public void close() throws Exception {
    frontLeft.close();
    frontRight.close();
    rearLeft.close();
    rearRight.close();
    gyro.close();
  }
}
