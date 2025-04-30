/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package SoarBridge;

import java.util.Objects;

/**
 *
 * @author Danilo
 */
public class Command {

    public enum CommandType {
        MOVE,
        GET,
        EAT,
        TIE,
        DELIVER,
        PLAN,

    }

    private CommandType commandType;
    private Object commandArgument;

    public Command() {
        commandType = null;
        commandArgument = null;
    }

    public Command(CommandType _command) {
        commandType = _command;
        switch (commandType) {
            case MOVE:
                commandArgument = new CommandMove();
                break;

            case GET:
                commandArgument = new CommandGet();
                break;

            case EAT:
                commandArgument = new CommandEat();
                break;
            case DELIVER:
                commandArgument = new CommandDeliver();
                break;

            case TIE:
                commandArgument = new CommandTie();
                break;

            case PLAN:
                commandArgument = new CommandPlan();
                break;
            default:
                commandArgument = null;
                break;
        }
    }

    /**
     * @return the command
     */
    public CommandType getCommandType() {
        return commandType;
    }

    /**
     * @param command the command to set
     */
    public void setCommand(CommandType command) {
        this.commandType = command;
    }

    /**
     * @return the commandArgument
     */
    public Object getCommandArgument() {
        return commandArgument;
    }

    /**
     * @param commandArgument the commandArgument to set
     */
    public void setCommandArgument(Object commandArgument) {
        this.commandArgument = commandArgument;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Command other = (Command) obj;

        if (this.commandType != other.commandType) {
            return false;
        }

        // Agora depende do tipo
        if (commandType == CommandType.GET) {
            CommandGet thisGet = (CommandGet) this.commandArgument;
            CommandGet otherGet = (CommandGet) other.commandArgument;
            return thisGet.getThingName().equals(otherGet.getThingName());
        }

        if (commandType == CommandType.MOVE) {
            CommandMove thisMove = (CommandMove) this.commandArgument;
            CommandMove otherMove = (CommandMove) other.commandArgument;
            return Float.compare(thisMove.getX(), otherMove.getX()) == 0
                    && Float.compare(thisMove.getY(), otherMove.getY()) == 0
                    && Float.compare(thisMove.getLinearVelocity(), otherMove.getLinearVelocity()) == 0
                    && Float.compare(thisMove.getLeftVelocity(), otherMove.getLeftVelocity()) == 0
                    && Float.compare(thisMove.getRightVelocity(), otherMove.getRightVelocity()) == 0;
        }

        return false;
    }

    @Override
    public int hashCode() {
        int result = commandType != null ? commandType.hashCode() : 0;

        if (commandType == CommandType.GET) {
            CommandGet thisGet = (CommandGet) commandArgument;
            result = 31 * result + (thisGet.getThingName() != null ? thisGet.getThingName().hashCode() : 0);
        } else if (commandType == CommandType.MOVE) {
            CommandMove thisMove = (CommandMove) commandArgument;
            result = 31 * result + Float.hashCode(thisMove.getX());
            result = 31 * result + Float.hashCode(thisMove.getY());
            result = 31 * result + Float.hashCode(thisMove.getLinearVelocity());
            result = 31 * result + Float.hashCode(thisMove.getLeftVelocity());
            result = 31 * result + Float.hashCode(thisMove.getRightVelocity());
        } else {
            return Objects.hash(commandType, commandArgument);
        }

        return result;
    }
}
