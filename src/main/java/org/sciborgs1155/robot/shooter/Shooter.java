package org.sciborgs1155.robot.shooter;

import org.sciborgs1155.robot.Ports;
import org.sciborgs1155.robot.Robot;

import edu.wpi.first.wpilibj2.command.Command;

public class Shooter {
    // Utilizes two motor objects rather than using one entire shooter for versitility.
    private final ShooterIO shooterTop;
    private final ShooterIO shooterBottom;

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
}