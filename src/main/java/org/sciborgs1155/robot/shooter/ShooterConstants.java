package org.sciborgs1155.robot.shooter;

import static edu.wpi.first.units.Units.Minute;
import static edu.wpi.first.units.Units.RadiansPerSecond;
import static edu.wpi.first.units.Units.Rotations;

import edu.wpi.first.units.measure.Angle;
import edu.wpi.first.units.measure.AngularVelocity;

public class ShooterConstants {
  public static final AngularVelocity DEFAULT_VELOCITY = RadiansPerSecond.of(550);
  public static final AngularVelocity MAX_VELOCITY = RadiansPerSecond.of(630);

  public static final Angle POSITION_FACTOR = Rotations.one();
  public static final AngularVelocity VELOCITY_FACTOR = POSITION_FACTOR.per(Minute);

  public static final double kP = 0.03;
  public static final double kI = 0.0;
  public static final double kD = 0.0;

  public static final class Top {
    public static final double kS = 0;
    public static final double kV = 0.016896;
    public static final double kA = 0.0031483;
  }

  public static final class Bottom {
    public static final double kS = 0.038488;
    public static final double kV = 0.016981;
    public static final double kA = 0.0021296;
  }
}
