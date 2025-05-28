package org.sciborgs1155.robot.shooter;

public class NoShooterMotor implements ShooterIO {
    // Constructor for no motor
    public NoShooterMotor() {}

    @Override
    public void setVoltage(double Voltage) {}
    
    @Override
    public double getVelocity() {
        return 0;
    }
}
