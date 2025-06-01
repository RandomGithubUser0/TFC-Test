package org.sciborgs1155.robot.shooter;

import org.sciborgs1155.robot.Ports;
import org.sciborgs1155.robot.Robot;
import org.sciborgs1155.robot.shooter.ShooterConstants.Bottom;
import org.sciborgs1155.robot.shooter.ShooterConstants.Top;

import static org.sciborgs1155.robot.shooter.ShooterConstants.kP;

import java.util.function.DoubleSupplier;

import static org.sciborgs1155.robot.shooter.ShooterConstants.kI;
import static edu.wpi.first.units.Units.RadiansPerSecond;
import static org.sciborgs1155.robot.shooter.ShooterConstants.DEFAULT_VELOCITY;
import static org.sciborgs1155.robot.shooter.ShooterConstants.MAX_VELOCITY;
import static org.sciborgs1155.robot.shooter.ShooterConstants.kD;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.controller.SimpleMotorFeedforward;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import monologue.Logged;
import monologue.Annotations.Log;

public class Shooter extends SubsystemBase implements AutoCloseable, Logged {
    // Utilizes two motor objects rather than using one entire shooter for versitility.
    private final ShooterIO shooterTop;
    private final ShooterIO shooterBottom;

    // Setpoints for the shooter motors
    @Log.NT private double topSetpoint;
    @Log.NT private double bottomSetpoint;

    // Create the FeedForward objects for the shooter motors.
    private final SimpleMotorFeedforward topFeedForward =
        new SimpleMotorFeedforward(Top.kS, Top.kV, Top.kA);
    private final SimpleMotorFeedforward bottomFeedForward =
        new SimpleMotorFeedforward(Bottom.kS, Bottom.kV, Bottom.kA); 

    // Create the PID objects for the shooter motors.
    @Log.NT private final PIDController topPID = new PIDController(kP, kI, kD);
    @Log.NT private final PIDController bottomPID = new PIDController(kP, kI, kD);

    // Factory method for constructing
    public static Shooter create(){
        return Robot.isReal() 
        ? new Shooter( // Inversion to make shooter properly shoot
            new RealShooterMotor(Ports.Shooter.TOP_MOTOR, false),
            new RealShooterMotor(Ports.Shooter.BOTTOM_MOTOR, true)
        ) 
        : new Shooter(
            new SimShooterMotor(),
            new SimShooterMotor()
        );
    }

    // Factory method for fake shooter
    public static Shooter fake() {
        return new Shooter(new NoShooterMotor(), new NoShooterMotor());
    }

    // Main constructor
    public Shooter(ShooterIO shooterTop, ShooterIO shooterBottom) {
        this.shooterTop = shooterTop;
        this.shooterBottom = shooterBottom;
    }

    public void setVoltage(ShooterIO motor, double voltage) {
        motor.setVoltage(voltage);
    }

    @Log.NT
    public double getVelocity(ShooterIO motor) {
        return motor.getVelocity();
    }

    public double calculateVelocity(double velocitySetPoint) {
        return Double.isNaN(velocitySetPoint)
            ? DEFAULT_VELOCITY.in(RadiansPerSecond)
            : MathUtil.clamp(
                velocitySetPoint,
                -MAX_VELOCITY.in(RadiansPerSecond),
                MAX_VELOCITY.in(RadiansPerSecond));
    }

    // Periodic method used to give the motors voltage based on PID and FeedForward
    public void update(double velocitySetPointTop, double velocitySetPointBottom) {
        double velocityTop = calculateVelocity(velocitySetPointTop);
        double velocityBottom;
        if (velocitySetPointBottom == velocitySetPointTop){ // If statement to prevent redundant calculations
            velocityBottom = velocityTop;
        } else {
            velocityBottom = calculateVelocity(velocitySetPointBottom);
        }
        double topFF = topFeedForward.calculate(velocityTop);
        double topPIDOut = topPID.calculate(shooterTop.getVelocity(), velocityTop);
        double bottomFF = bottomFeedForward.calculate(velocityBottom);
        double bottomPIDOut = bottomPID.calculate(shooterBottom.getVelocity(), velocityBottom);
        log("top output", topFF + topPIDOut);
        log("bottom output", bottomFF + bottomPIDOut);

        shooterTop.setVoltage(MathUtil.clamp(topFF + topPIDOut, -12, 12));
        shooterBottom.setVoltage(MathUtil.clamp(bottomFF + bottomPIDOut, -12, 12));

        topSetpoint = velocityTop;
        bottomSetpoint = velocityBottom;
    }

    /**
     * Run the shooter at a specified velocity.
     *
     * @param velocity The desired velocity in radians per second.
     * @return The command to set the shooter's velocity.
     */
    public Command runShooter(DoubleSupplier velocityTop, DoubleSupplier velocityBottom) {
        return run(() -> update(
            velocityTop.getAsDouble(),
            velocityBottom.getAsDouble()
        )).withName("running shooter");
    }

    @Override
    public void close() throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'close'");
    }
}