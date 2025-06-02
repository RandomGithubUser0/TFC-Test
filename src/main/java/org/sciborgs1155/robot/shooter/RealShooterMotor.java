package org.sciborgs1155.robot.shooter;
import com.revrobotics.spark.SparkBase.PersistMode;
import com.revrobotics.spark.SparkBase.ResetMode;
import com.revrobotics.RelativeEncoder;
import com.revrobotics.ColorSensorV3.Register;
import com.revrobotics.spark.SparkFlex;
import com.revrobotics.spark.SparkMax;
import com.revrobotics.spark.SparkLowLevel.MotorType;
import com.revrobotics.spark.config.SparkBaseConfig.IdleMode;
import com.revrobotics.spark.config.SparkMaxConfig;

import static org.sciborgs1155.robot.Ports.Shooter.*;

public class RealShooterMotor implements ShooterIO {
    private final SparkFlex motor;
    private final RelativeEncoder encoder;

    // Constructor for motor
    public RealShooterMotor(int id, boolean inversion) { // id is the thing from ports
        SparkMaxConfig config = new SparkMaxConfig();

        motor = new SparkFlex(id, MotorType.kBrushless);
        motor.configure(config, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);    

        encoder = motor.getEncoder();

        config.inverted(inversion);
    }

    @Override
    public void setVoltage(double Voltage) {
        motor.set(Voltage);
    }

    @Override
    public double getVelocity(){
        return encoder.getVelocity();
    }

    @Override
    public void close() throws Exception {
        motor.close();
    }
}
