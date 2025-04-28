/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package SoarBridge;

import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;

/**
 *
 * @author karenbaliero
 */
public class CommandImpasseInfo
{
    Symbol impasseInfoIdentifier;

    public CommandImpasseInfo()
    {

    }
    
    public void setImpasseInfoIdentifier(Symbol value) {
        this.impasseInfoIdentifier = value;
    }
    
    public Symbol getImpasseInfoIdentifier() {
        return impasseInfoIdentifier;
    }

}
