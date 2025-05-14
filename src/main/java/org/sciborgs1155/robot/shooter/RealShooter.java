package org.sciborgs1155.robot.shooter;
import com.revrobotics.spark.SparkMax;
import com.revrobotics.spark.SparkLowLevel.MotorType;

import static org.sciborgs1155.robot.Ports.Shooter.*;

public class RealShooter implements ShooterIO {
    private final SparkMax leftLeader = new SparkMax(LEFT_LEADER, MotorType.kBrushless);
    private final SparkMax leftFollower = new SparkMax(LEFT_FOLLOWER, MotorType.kBrushless);

    @Override
    public void setVoltage(double Voltage) {
        
    }
}
