package org.sciborgs1155.robot.shooter;

public class SimShooterMotor implements ShooterIO {
    // Constructor for sim motor
    public SimShooterMotor() {}

    @Override
    public void setVoltage(double Voltage) {}

    @Override
    public double getVelocity() {
        return 0;
    }

    @Override
    public void close() {
        // No resources to close in simulation
    }
}
