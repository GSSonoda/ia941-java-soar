/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package SoarBridge;

/**
 *
 * @author Danilo
 */
public class CommandMove
{
    
    private Float rightVelocity = null;
    private Float leftVelocity = null;
    private Float linearVelocity = null;
    private Float x = null;
    private Float y = null;

    public CommandMove()
    {

    }
    
    public CommandMove(Double xVal, Double yVal, Double vel, Double velL, Double velR)
    {
     x = xVal.floatValue();
     y = yVal.floatValue();
     linearVelocity = vel.floatValue();
     leftVelocity = velL.floatValue();
     rightVelocity = velR.floatValue();
    }

    /**
     * @return the rightVelocity
     */
    public float getRightVelocity()
    {
        return rightVelocity;
    }

    /**
     * @param rightVelocity the rightVelocity to set
     */
    public void setRightVelocity(float rightVelocity)
    {
        this.rightVelocity = rightVelocity;
    }

    /**
     * @return the leftVelocity
     */
    public float getLeftVelocity()
    {
        return leftVelocity;
    }

    /**
     * @param leftVelocity the leftVelocity to set
     */
    public void setLeftVelocity(float leftVelocity)
    {
        this.leftVelocity = leftVelocity;
    }

    /**
     * @return the wheel
     */
    public float getLinearVelocity()
    {
        return linearVelocity;
    }

    /**
     * @param wheel the wheel to set
     */
    public void setLinearVelocity(float linearVelocity)
    {
        this.linearVelocity = linearVelocity;
    }

    /**
     * @return the x Position
     */
    public Float getX()
    {
        return x;
    }

    /**
     * @param xPosition the x Position to set
     */
    public void setX(Float xPosition)
    {
        this.x = xPosition;
    }

    /**
     * @return the yPosition
     */
    public Float getY()
    {
        return y;
    }

    /**
     * @param yPosition the y Position to set
     */
    public void setY(Float yPosition)
    {
        this.y = yPosition;
    }


}
