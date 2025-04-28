/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package SoarBridge;

import org.jsoar.kernel.symbols.Symbol;

/**
 *
 * @author karenbaliero
 */
public class CommandInputLink
{

    Symbol InputLinkIdentifier;

    public CommandInputLink()
    {

    }
    
    public void setInputLinkIdentifier(Symbol value) {
        this.InputLinkIdentifier = value;
    }
    
    public Symbol getInputLinkIdentifier() {
        return InputLinkIdentifier;
    }

}
