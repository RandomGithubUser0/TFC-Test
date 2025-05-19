package org.sciborgs1155.robot.shooter;

public class NoShooterMotor implements ShooterIO {
    @Override
    public void setVoltage(double Voltage) {}
    
    @Override
    public double getSpeed() {
        return 0;
    }
}
