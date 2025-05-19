package org.sciborgs1155.robot.shooter;

public class Shooter {
    private final ShooterIO shooterTop;
    private final ShooterIO shooterBottom;

    // Factory method for constructing
    public static Shooter create(){
        return new Shooter(new RealShooterMotor(), new RealShooterMotor());
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