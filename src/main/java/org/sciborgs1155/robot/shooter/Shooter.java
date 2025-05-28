package org.sciborgs1155.robot.shooter;

import org.sciborgs1155.robot.Ports;
import org.sciborgs1155.robot.Robot;
import org.sciborgs1155.robot.shooter.ShooterConstants.Bottom;
import org.sciborgs1155.robot.shooter.ShooterConstants.Top;

import static org.sciborgs1155.robot.shooter.ShooterConstants.kP;
import static org.sciborgs1155.robot.shooter.ShooterConstants.kI;
import static org.sciborgs1155.robot.shooter.ShooterConstants.kD;

import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.controller.SimpleMotorFeedforward;

import monologue.Annotations.Log;

public class Shooter {
    // Utilizes two motor objects rather than using one entire shooter for versitility.
    private final ShooterIO shooterTop;
    private final ShooterIO shooterBottom;

    // Create the FeedForward objects for the shooter motors.
    private final SimpleMotorFeedforward topFeedForward =
        new SimpleMotorFeedforward(Top.kS, Top.kV, Top.kA);
    private final SimpleMotorFeedforward bottomFeedForward =
        new SimpleMotorFeedforward(Bottom.kS, Bottom.kV, Bottom.kA); 

    // Create the PID objects for the shooter motors.
    @Log.NT private final PIDController topPID = new PIDController(kP, kI, kD);

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

    public void setBothVoltage(double voltage) {
        shooterTop.setVoltage(voltage);
        shooterBottom.setVoltage(voltage);
    }

    public void setIndependentVoltage(ShooterIO motor, double voltage) {
        motor.setVoltage(voltage);
    }

    @Log.NT
    public double topVelocity() {
        return shooterTop.getVelocity();
    }

    @Log.NT
    public double bottomVelocity() {
        return shooterBottom.getVelocity();
    }

    // Periodic method used to give the motors voltage based on PID and FeedForward
    public void update(double velocitySetPoint) {
        // Clamp Velocity using ?:
        // FF and PID Calculate
        // Log the outputs
        // Lastly set the voltage
        // Change the setpoint to velocity
    }
}