package org.sciborgs1155.robot.shooter;
import com.revrobotics.spark.SparkMax;
import com.revrobotics.spark.SparkLowLevel.MotorType;

import static org.sciborgs1155.robot.Ports.Shooter.*;

public class RealShooterMotor implements ShooterIO {
    private final SparkMax topMotor = new SparkMax(TOP_MOTOR, MotorType.kBrushless);
    private final SparkMax bottomMotor = new SparkMax(BOTTOM_MOTOR, MotorType.kBrushless);

    @Override
    public void setVoltage(double Voltage) {
        topMotor.set(Voltage);
        bottomMotor.set(Voltage);
    }

    @Override
    public double getSpeed(){
        return topMotor.get();
    }
}
