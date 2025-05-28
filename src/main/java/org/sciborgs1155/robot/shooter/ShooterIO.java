package org.sciborgs1155.robot.shooter;

public interface ShooterIO {
    /**
     * Sets the velocity of the two motors for shooting.
     * 
     * @param voltage Voltage of motor. 
    **/
    void setVoltage(double voltage);

    /**
     * Gets the velocity of the flywheel in radians / sec. 
     * 
     * @return The velocity of the flywheel in radians / sec.
     */
    double getVelocity();
}
