/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package SoarBridge;

import Simulation.Environment;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.Map;
import java.util.HashMap;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Phase;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.memory.Wmes;
import org.jsoar.kernel.symbols.DoubleSymbol;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.StringSymbol;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.runtime.ThreadedAgent;
import org.jsoar.util.commands.SoarCommands;
import ws3dproxy.CommandExecException;
import ws3dproxy.CommandUtility;
import ws3dproxy.model.Creature;
import ws3dproxy.model.Thing;
import ws3dproxy.model.Leaflet;
import ws3dproxy.model.Bag;
import ws3dproxy.util.Constants;

/**
 *
 * @author Danilo Lucentini and Ricardo Gudwin
 */
public class SoarBridge
{
    // Log Variable
    Logger logger = Logger.getLogger(SoarBridge.class.getName());

    // SOAR Variables
    Agent agent = null;
    public Identifier inputLink = null;

    // Entity Variables
    Identifier creature;
    Identifier creatureSensor;
    Identifier creatureParameters;
    Identifier creaturePosition;
    Identifier creatureMemory;
    
    Environment env;
    public Creature c;
    public String input_link_string = "";
    public String output_link_string = "";

    /**
     * @autor g.sonoda
     * Inicialização de variaveis uteis para a aula 6 - atividade 1
     */
    private Bag bag = new Bag(0, 0, 0, 0, new ArrayList<Integer>());
    private Map<String, Thing> knownFoods = new HashMap<>();
    private Map<String, Thing> knownJewels = new HashMap<>();
    private Leaflet l1;
    private Leaflet l2;
    private Leaflet l3;


    /**
     * Constructor class
     * @param _e Environment
     * @param path Path for Rule Base
     * @param startSOARDebugger set true if you wish the SOAR Debugger to be started
     */
    public SoarBridge(Environment _e, String path, Boolean startSOARDebugger) 
    {
        env = _e;
        c = env.getCreature();
        try
        {
            ThreadedAgent tag = ThreadedAgent.create();
            agent = tag.getAgent();
            SoarCommands.source(agent.getInterpreter(), path);
            inputLink = agent.getInputOutput().getInputLink();

            // Initialize entities
            creature = null;

            // Debugger line
            if (startSOARDebugger)
            {
                agent.openDebugger();
            }
        }
        catch (Exception e)
        {
            logger.severe("Error while creating SOAR Kernel");
            e.printStackTrace();
        }
    }

    private Identifier CreateIdWME(Identifier id, String s) {
        SymbolFactory sf = agent.getSymbols();
        Identifier newID = sf.createIdentifier('I');
        agent.getInputOutput().addInputWme(id, sf.createString(s), newID);
        return(newID);
    }
    
    private void CreateFloatWME(Identifier id, String s, double value) {
        SymbolFactory sf = agent.getSymbols();
        DoubleSymbol newID = sf.createDouble(value);
        agent.getInputOutput().addInputWme(id, sf.createString(s), newID);
    }
    
    private void CreateStringWME(Identifier id, String s, String value) {
        SymbolFactory sf = agent.getSymbols();
        StringSymbol newID = sf.createString(value);
        agent.getInputOutput().addInputWme(id, sf.createString(s), newID);
    }
    
    private String getItemType(int categoryType)
    {
        String itemType = null;

        switch (categoryType)
        {
            case Constants.categoryBRICK:
                itemType = "BRICK";
                break;
            case Constants.categoryJEWEL:
                itemType = "JEWEL";
                break;
            case Constants.categoryFOOD:
            case Constants.categoryNPFOOD:
            case Constants.categoryPFOOD:
                itemType = "FOOD";
                break;
            case Constants.categoryCREATURE:
                itemType = "CREATURE";
                break;
        }
        return itemType;
    }

    /**
     *
     * @author g.sonoda
     * Aula 6 - Atividade 1
     * Verificar se as joias conhecidas são suficientes para completar o leaflet
     */
    private boolean knownJewelsSatisfyLeaflet(Map<String, Thing> jewels, Leaflet leaflet) {
        if (jewels == null || jewels.isEmpty()) {
            return false;
        }

        // Contar quantas joias de cada cor temos em knownJewels
        Map<String, Integer> jewelColorCounts = new HashMap<>();
        for (Thing jewel : jewels.values()) {
            String color = Constants.getColorName(jewel.getMaterial().getColor());
            jewelColorCounts.put(color, jewelColorCounts.getOrDefault(color, 0) + 1);
        }

        HashMap<String, Integer[]> leaflet_map = leaflet.getItems();
        for (Map.Entry<String, Integer[]> entry : leaflet_map.entrySet()) {
            String type = entry.getKey();
            int requiredAmount = entry.getValue()[0]; // total necessário
            int availableJewels = jewelColorCounts.getOrDefault(type, 0); // quantidade disponível nas knownJewels

            if (availableJewels < requiredAmount) {
                return false;
            }
        }
        return true;
    }

    /**
     *
     * @author g.sonoda
     * Aula 6 - Atividade 1
     * Verificar se a bag da possui cristais suficientes para completar aquele leaflet
     */
    private boolean bagSatisfiesLeaflet(Bag bag, Leaflet leaflet) {
        if (bag == null) {
            return false;
        }
        HashMap<String, Integer[]> leaflet_map = leaflet.getItems();
        for (Map.Entry<String, Integer[]> entry : leaflet_map.entrySet()) {
            String type = entry.getKey();
            int requiredAmount = entry.getValue()[0]; // total necessário
            int availableInBag = bag.getNumberCrystalPerType(type); // quantidade na bag
    
            if (availableInBag < requiredAmount) {
                return false;
            }
        }
    
        return true;
    }

    /**
     *
     * @author g.sonoda
     * Aula 6 - Atividade 1
     * Verificar se a cor está presente em algum leaflet
     */
    private boolean isColorInLeaflets(String color) {
        return l1.getItems().containsKey(color) || l2.getItems().containsKey(color) || l3.getItems().containsKey(color);
    }

    /**
     * @autor g.sonoda
     * Incluir novos InputLinks como
     * LONGMEMORY
     * LEAFLEAT
     * CLOSESTFOOD
     * CLOSESTJEWEL
     */
    /**
     * Create the WMEs at the InputLink of SOAR
     */
    private void prepareInputLink() 
    {
        //SymbolFactory sf = agent.getSymbols();
        Creature c = env.getCreature();
        
        l1 = c.getLeaflets().get(0);
        l2 = c.getLeaflets().get(1);
        l3 = c.getLeaflets().get(2);        
        inputLink = agent.getInputOutput().getInputLink();
        try
        {
            if (agent != null)
            {
              //SimulationCreature creatureParameter = (SimulationCreature)parameter;
              // Initialize Creature Entity
              creature = CreateIdWME(inputLink, "CREATURE");
              // Initialize Creature Memory
              creatureMemory = CreateIdWME(creature, "MEMORY");
              // Set Creature Parameters
              Calendar lCDateTime = Calendar.getInstance();
              creatureParameters = CreateIdWME(creature, "PARAMETERS");
              CreateFloatWME(creatureParameters, "MINFUEL", 300);
              CreateFloatWME(creatureParameters, "TIMESTAMP", lCDateTime.getTimeInMillis());
              // Setting creature Position
              creaturePosition = CreateIdWME(creature, "POSITION");
              CreateFloatWME(creaturePosition, "X", c.getPosition().getX());
              CreateFloatWME(creaturePosition, "Y", c.getPosition().getY());
              // Set creature sensors
              creatureSensor = CreateIdWME(creature, "SENSOR");
              // Create Fuel Sensors
              Identifier fuel = CreateIdWME(creatureSensor, "FUEL");
              CreateFloatWME(fuel, "VALUE",c.getFuel());
              // Create Visual Sensors
              Identifier visual = CreateIdWME(creatureSensor, "VISUAL");
              List<Thing> thingsList = (List<Thing>) c.getThingsInVision();
              for (Thing t : thingsList) 
                {
                    Identifier entity = CreateIdWME(visual, "ENTITY");
                    CreateFloatWME(entity, "DISTANCE", GetGeometricDistanceToCreature(t.getX1(),t.getY1(),t.getX2(),t.getY2(),c.getPosition().getX(),c.getPosition().getY()));                                                    
                    CreateFloatWME(entity, "X", t.getX1());
                    CreateFloatWME(entity, "Y", t.getY1());
                    CreateFloatWME(entity, "X2", t.getX2());
                    CreateFloatWME(entity, "Y2", t.getY2());
                    CreateStringWME(entity, "TYPE", getItemType(t.getCategory()));
                    CreateStringWME(entity, "NAME", t.getName());
                    CreateStringWME(entity, "COLOR",Constants.getColorName(t.getMaterial().getColor()));
                    String type = getItemType(t.getCategory());
                    String name = t.getName();                    
                    if ("FOOD".equals(type)) {
                        knownFoods.putIfAbsent(name, t); // armazena só se ainda não conhece
                    } else if ("JEWEL".equals(type)) {
                        knownJewels.putIfAbsent(name, t);
                    }                       
                }
                Identifier creatureLongMemory = CreateIdWME(creature, "LONGMEMORY");
                Identifier closestFoodWME = CreateIdWME(creatureLongMemory, "CLOSESTFOOD");
                Identifier closestJewelWME = CreateIdWME(creatureLongMemory, "CLOSESTJEWEL");

                double closestFoodDistance = Double.MAX_VALUE;
                double closestJewelDistance = Double.MAX_VALUE;
                Thing closestFood = null;
                Thing closestJewel = null;


                for (Thing food : knownFoods.values()) {
                    double foodDistance = GetGeometricDistanceToCreature(food.getX1(), food.getY1(), food.getX2(), food.getY2(), c.getPosition().getX(), c.getPosition().getY());
                    Identifier foodWME = CreateIdWME(creatureLongMemory, "ENTITY");
                    CreateStringWME(foodWME, "NAME", food.getName());
                    CreateStringWME(foodWME, "TYPE", "FOOD");
                    CreateFloatWME(foodWME, "X", (float) food.getX1());
                    CreateFloatWME(foodWME, "Y", (float) food.getY1());
                    if (foodDistance < closestFoodDistance) {
                        closestFoodDistance = foodDistance;
                        closestFood = food;
                    }
                }
                for (Thing jewel : knownJewels.values()) {
                    double jewelDistance = GetGeometricDistanceToCreature(jewel.getX1(), jewel.getY1(), jewel.getX2(), jewel.getY2(), c.getPosition().getX(), c.getPosition().getY());
                    String color = Constants.getColorName(jewel.getMaterial().getColor());
                    Identifier jewelsWME = CreateIdWME(creatureLongMemory, "ENTITY");
                    CreateStringWME(jewelsWME, "NAME", jewel.getName());
                    CreateStringWME(jewelsWME, "TYPE", "JEWEL");
                    CreateFloatWME(jewelsWME, "X", (float) jewel.getX1());
                    CreateFloatWME(jewelsWME, "Y", (float) jewel.getY1());
                    CreateStringWME(jewelsWME, "COLOR", color);
                    
                    if (!isColorInLeaflets(color)) {
                        continue;
                    }
                
                    if (jewelDistance < closestJewelDistance ) {
                        closestJewelDistance = jewelDistance;
                        closestJewel = jewel;
                    }
                }
                if (closestFood != null) {
                    CreateStringWME(closestFoodWME, "NAME", closestFood.getName());
                    CreateFloatWME(closestFoodWME, "X", (float) closestFood.getX1());
                    CreateFloatWME(closestFoodWME, "Y", (float) closestFood.getY1());
                }
                if (closestJewel != null) {

                    CreateStringWME(closestJewelWME, "NAME", closestJewel.getName());
                    CreateFloatWME(closestJewelWME, "X", (float) closestJewel.getX1());
                    CreateFloatWME(closestJewelWME, "Y", (float) closestJewel.getY1());
                    CreateStringWME(closestJewelWME, "COLOR", Constants.getColorName(closestJewel.getMaterial().getColor()));
                }                
                // bag = c.updateBag();
                // Identifier leaflet = CreateIdWME(creature, "LEAFLET");
                // Identifier leaflet_l1 = CreateIdWME(leaflet, "L1");
                // Identifier leaflet_l2 = CreateIdWME(leaflet, "L2");
                // Identifier leaflet_l3 = CreateIdWME(leaflet, "L3");
                // CreateFloatWME(leaflet_l1, "PAYMENT", l1.getPayment());
                // CreateFloatWME(leaflet_l2, "PAYMENT", l2.getPayment());
                // CreateFloatWME(leaflet_l3, "PAYMENT", l3.getPayment());
                // CreateFloatWME(leaflet_l1, "PLAY", knownJewelsSatisfyLeaflet(knownJewels, l1) ? 1.0f : 0.0f);
                // CreateFloatWME(leaflet_l2, "PLAY", knownJewelsSatisfyLeaflet(knownJewels, l2) ? 1.0f : 0.0f);
                // CreateFloatWME(leaflet_l3, "PLAY", knownJewelsSatisfyLeaflet(knownJewels, l3) ? 1.0f : 0.0f);

                // CreateFloatWME(leaflet, "L1READY", bagSatisfiesLeaflet(bag, l1) ? 1.0f : 0.0f);
                // CreateFloatWME(leaflet, "L2READY", bagSatisfiesLeaflet(bag, l2) ? 1.0f : 0.0f);
                // CreateFloatWME(leaflet, "L3READY", bagSatisfiesLeaflet(bag, l3) ? 1.0f : 0.0f);
                for (Leaflet l : c.getLeaflets()) {
                    Identifier leaflet = CreateIdWME(creature, "LEAFLET");
                    CreateStringWME(leaflet, "ID", l.getID().toString());
                    CreateFloatWME(leaflet, "PAYMENT", l.getPayment());
                    CreateFloatWME(leaflet, "SITUATION", l.getSituation());
                    for (Iterator<String> iter = l.getItems().keySet().iterator(); iter.hasNext();) {
                        String color = iter.next();
                        int needed = l.getItems().get(color)[0];
                        Identifier item = CreateIdWME(leaflet, "ITEM");
                        CreateStringWME(item, "COLOR", color);
                        CreateFloatWME(item, "NEEDED", needed);
                    }
                }    

              c.updateBag();
              Bag creatureBag = c.getBag();
              if(creatureBag != null) {
                Identifier bag = CreateIdWME(creature, "BAG");
                Map<String, String> bagMap = creatureBag.getMap();
                createBagItem(bag, "Red", Float.parseFloat(bagMap.get(Constants.TOKEN_BAG_CRYSTAL_RED)));
                createBagItem(bag, "Green", Float.parseFloat(bagMap.get(Constants.TOKEN_BAG_CRYSTAL_GREEN)));
                createBagItem(bag, "Blue", Float.parseFloat(bagMap.get(Constants.TOKEN_BAG_CRYSTAL_BLUE)));
                createBagItem(bag, "Yellow", Float.parseFloat(bagMap.get(Constants.TOKEN_BAG_CRYSTAL_YELLOW)));
                createBagItem(bag, "Magenta", Float.parseFloat(bagMap.get(Constants.TOKEN_BAG_CRYSTAL_MAGENTA)));
                createBagItem(bag, "White", Float.parseFloat(bagMap.get(Constants.TOKEN_BAG_CRYSTAL_WHITE)));
              }  
            }
        }
        catch (Exception e)
        {
            logger.severe("Error while Preparing Input Link");
            e.printStackTrace();
        }
    }
    private void createBagItem(Identifier bag, String color, float count) {
        Identifier item = CreateIdWME(bag, "ITEM");
        CreateStringWME(item, "COLOR", color);
        CreateFloatWME(item, "COUNT", count);
    }

    private double GetGeometricDistanceToCreature(double x1, double y1, double x2, double y2, double xCreature, double yCreature)
    {
          float squared_dist = 0.0f;
          double maxX = Math.max(x1, x2);
          double minX = Math.min(x1, x2);
          double maxY = Math.max(y1, y2);
          double minY = Math.min(y1, y2);

          if(xCreature > maxX)
          {
            squared_dist += (xCreature - maxX)*(xCreature - maxX);
          }
          else if(xCreature < minX)
          {
            squared_dist += (minX - xCreature)*(minX - xCreature);
          }

          if(yCreature > maxY)
          {
            squared_dist += (yCreature - maxY)*(yCreature - maxY);
          }
          else if(yCreature < minY)
          {
            squared_dist += (minY - yCreature)*(minY - yCreature);
          }

          return Math.sqrt(squared_dist);
    }

    private void resetSimulation() {
        agent.initialize();
    }
    
    /**
     * Run SOAR until HALT
     */
    private void runSOAR() 
    {
        agent.runForever(); 
    }
    
    private int stepSOAR() {
        agent.runFor(1, RunType.PHASES);
        Phase ph = agent.getCurrentPhase();
        if (ph.equals(Phase.INPUT)) return(0);
        else if (ph.equals(Phase.PROPOSE)) return(1);
        else if (ph.equals(Phase.DECISION)) return(2);
        else if (ph.equals(Phase.APPLY)) return(3);
        else if (ph.equals(Phase.OUTPUT)) {
            if (agent.getReasonForStop() == null) return(4);
            else return(5);
        }
        else return(6);
    }

    private String GetParameterValue(String par) {
        List<Wme> Commands = Wmes.matcher(agent).filter(agent.getInputOutput().getOutputLink());
        List<Wme> Parameters = Wmes.matcher(agent).filter(Commands.get(0));
        String parvalue = "";
        for (Wme w : Parameters) 
           if (w.getAttribute().toString().equals(par)) parvalue = w.getValue().toString();
        return(parvalue);
    }
    
    /**
     * @autor g.sonoda
     * Incluir DELIVER nos tipos de OutputLink
     */
    /**
     * Process the OutputLink given by SOAR and return a list of commands to WS3D
     * @return A List of SOAR Commands
     */
    private ArrayList<Command> processOutputLink() 
    {
        ArrayList<Command> commandList = new ArrayList<Command>();

        try
        {
            if (agent != null)
            {
                List<Wme> Commands = Wmes.matcher(agent).filter(agent.getInputOutput().getOutputLink());

                for (Wme com : Commands)
                {
                    String name  = com.getAttribute().asString().getValue();
                    Command.CommandType commandType = Enum.valueOf(Command.CommandType.class, name);
                    Command command = null;

                    switch(commandType)
                    {
                        case MOVE:
                            Float rightVelocity = null;
                            Float leftVelocity = null;
                            Float linearVelocity = null;
                            Float xPosition = null;
                            Float yPosition = null;
                            rightVelocity = tryParseFloat(GetParameterValue("VelR"));
                            leftVelocity = tryParseFloat(GetParameterValue("VelL"));
                            linearVelocity = tryParseFloat(GetParameterValue("Vel"));
                            xPosition = tryParseFloat(GetParameterValue("X"));
                            yPosition = tryParseFloat(GetParameterValue("Y"));
                            command = new Command(Command.CommandType.MOVE);
                            CommandMove commandMove = (CommandMove)command.getCommandArgument();
                            if (commandMove != null)
                            {
                                if (rightVelocity != null) commandMove.setRightVelocity(rightVelocity);
                                if (leftVelocity != null)  commandMove.setLeftVelocity(leftVelocity);
                                if (linearVelocity != null) commandMove.setLinearVelocity(linearVelocity);
                                if (xPosition != null) commandMove.setX(xPosition);
                                if (yPosition != null) commandMove.setY(yPosition);
                                commandList.add(command);
                            }
                            else
                            {
                                logger.severe("Error processing MOVE command");
                            }
                            break;

                        case GET:
                            String thingNameToGet = null;
                            command = new Command(Command.CommandType.GET);
                            CommandGet commandGet = (CommandGet)command.getCommandArgument();
                            if (commandGet != null)
                            {
                                thingNameToGet = GetParameterValue("Name");
                                if (thingNameToGet != null) commandGet.setThingName(thingNameToGet);
                                commandList.add(command);
                            }
                            break;

                        case EAT:
                            String thingNameToEat = null;
                            command = new Command(Command.CommandType.EAT);
                            CommandEat commandEat = (CommandEat)command.getCommandArgument();
                            if (commandEat != null)
                            {
                                thingNameToEat = GetParameterValue("Name");
                                if (thingNameToEat != null) commandEat.setThingName(thingNameToEat);
                                commandList.add(command);
                            }
                            break;
                        case DELIVER:
                            command = new Command(Command.CommandType.DELIVER);
                            commandList.add(command);
                            break;

                        default:
                            break;
                    }   
                }
            }
        }
        catch (Exception e)
        {
            logger.severe("Error while processing commands");
            e.printStackTrace();
        }

        return ((commandList.size() > 0) ? commandList : null);
    }
    
    /**
     * Perform a complete SOAR step
     * @throws ws3dproxy.CommandExecException
     */
    public void step() throws CommandExecException
    {
        if (phase != -1) finish_msteps();
        resetSimulation();
        c.updateState();

        prepareInputLink();
        input_link_string = stringInputLink();
        //printInputWMEs();
        runSOAR();
        output_link_string = stringOutputLink();
        // printOutputWMEs();
        List<Command> commandList = processOutputLink();
        processCommands(commandList);
        //resetSimulation();
    }
    
    
    public void prepare_mstep() {
        resetSimulation();
        c.updateState();
        prepareInputLink();
        input_link_string = stringInputLink();
    }
    
    public int phase=-1;
    public void mstep() throws CommandExecException
    {
        if (phase == -1) prepare_mstep();
        phase = stepSOAR();
        if (phase == 5) {
            post_mstep();
            phase = -1;
        }
    }
    
    public void finish_msteps() throws CommandExecException {
        while (phase != -1) mstep();
    }
    
    public void post_mstep() throws CommandExecException {
        output_link_string = stringOutputLink();
        //printOutputWMEs();
        List<Command> commandList = processOutputLink();
        processCommands(commandList);
        //resetSimulation();
    }

    /**
     * @autor g.sonoda
     * Incluir DELIVER nos tipos de processo
     */
    private void processCommands(List<Command> commandList) throws CommandExecException
    {

        if (commandList != null)
        {
            for (Command command:commandList)
            {
                switch (command.getCommandType())
                {
                    case MOVE:
                        processMoveCommand((CommandMove)command.getCommandArgument());
                    break;

                    case GET:
                        processGetCommand((CommandGet)command.getCommandArgument());
                    break;

                    case EAT:
                        processEatCommand((CommandEat)command.getCommandArgument());
                    break;
                    case DELIVER:
                        processDeliverCommand((CommandDeliver)command.getCommandArgument());
                    break;

                    default:
                        System.out.println("Nenhum comando definido ...");
                        // Do nothing
                    break;
                }
            }
        }
        else System.out.println("comando nulo ...");
    }

    /**
     * Send Move Command to World Server
     * @param soarCommandMove Soar Move Command Structure
     */
    private void processMoveCommand(CommandMove soarCommandMove) throws CommandExecException
    {
        if (soarCommandMove != null)
        {
            if (soarCommandMove.getX() != null && soarCommandMove.getY() != null)
            {
                CommandUtility.sendGoTo("0", soarCommandMove.getRightVelocity(), soarCommandMove.getLeftVelocity(), soarCommandMove.getX(), soarCommandMove.getY());
            }
            else
            {
                CommandUtility.sendSetTurn("0",soarCommandMove.getLinearVelocity(),soarCommandMove.getRightVelocity(),soarCommandMove.getLeftVelocity());
            }
        }
        else
        {
            logger.severe("Error processing processMoveCommand");
        }
    }

    /**
     * Send Get Command to World Server
     * @param soarCommandGet Soar Get Command Structure
     */
    private void processGetCommand(CommandGet soarCommandGet) throws CommandExecException
    {
        if (soarCommandGet != null)
        {
            c.putInSack(soarCommandGet.getThingName());
            /**
             *
             * @author g.sonoda
             * Aula 6 - Atividade 1
             * Remover Joia da memoria
             */
            knownJewels.remove(soarCommandGet.getThingName());
        }
        else
        {
            logger.severe("Error processing processMoveCommand");
        }
    }

     /**
     * Send Eat Command to World Server
     * @param soarCommandEat Soar Eat Command Structure
     */
    private void processEatCommand(CommandEat soarCommandEat) throws CommandExecException
    {
        if (soarCommandEat != null)
        {
            c.eatIt(soarCommandEat.getThingName());
            /**
             *
             * @author g.sonoda
             * Aula 6 - Atividade 1
             * Remover Joia da memoria
             */
            knownFoods.remove(soarCommandEat.getThingName());
        }
        else
        {
            logger.severe("Error processing processMoveCommand");
        }
    }

    /**
     * @autor g.sonoda
     * Trocar joias por pontos
     */
    private void processDeliverCommand(CommandDeliver soarCommandDeliver) throws CommandExecException {
        if (bagSatisfiesLeaflet(bag, l1)){
            c.deliverLeaflet(l1.getID().toString());
            bag = c.updateBag();
        }
        if (bagSatisfiesLeaflet(bag, l2)){
            c.deliverLeaflet(l2.getID().toString());
            bag = c.updateBag();
        }
        if (bagSatisfiesLeaflet(bag, l3)){
            c.deliverLeaflet(l3.getID().toString());
            bag = c.updateBag();
        }
        
    }

    /**
     * Try Parse a Float Element
     * @param value Float Value
     * @return The Float Value or null otherwise
     */
    private Float tryParseFloat (String value)
    {
        Float returnValue = null;

        try
        {
            returnValue = Float.parseFloat(value);
        }
        catch (Exception ex)
        {
            returnValue = null;
        }

        return returnValue;
    }
    
    public void printWME(Identifier id) {
        printWME(id,0);
        
    }
    
    public void printWME(Identifier id, int level) {
        Iterator<Wme> It = id.getWmes();
        while (It.hasNext()) {
            Wme wme = It.next();
            Identifier idd = wme.getIdentifier();
            Symbol a = wme.getAttribute();
            Symbol v = wme.getValue();
            Identifier testv = v.asIdentifier();
            for (int i=0;i<level;i++) System.out.print("   ");
            if (testv != null) {
                System.out.print("("+idd.toString()+","+a.toString()+","+v.toString()+")\n");
                printWME(testv,level+1);
            }
            else System.out.print("("+idd.toString()+","+a.toString()+","+v.toString()+")\n");
        }   
    }
    
    public void printInputWMEs(){
        Identifier il = agent.getInputOutput().getInputLink();
        System.out.println("Input --->");
        printWME(il);
    }
    
    public void printOutputWMEs(){
        Identifier ol = agent.getInputOutput().getOutputLink();
        System.out.println("Output --->");
        printWME(ol);
    }
    
    public String stringWME(Identifier id) {
        String out = stringWME(id,0);
        return(out);
    }
    
    public String stringWME(Identifier id, int level) {
        String out="";
        Iterator<Wme> It = id.getWmes();
        while (It.hasNext()) {
            Wme wme = It.next();
            Identifier idd = wme.getIdentifier();
            Symbol a = wme.getAttribute();
            Symbol v = wme.getValue();
            Identifier testv = v.asIdentifier();
            for (int i=0;i<level;i++) out += "   ";
            if (testv != null) {
                out += "("+idd.toString()+","+a.toString()+","+v.toString()+")\n";
                out += stringWME(testv,level+1);
            }
            else out += "("+idd.toString()+","+a.toString()+","+v.toString()+")\n";
        }
       return(out); 
    }
    
    public String stringInputLink() {
        Identifier il = agent.getInputOutput().getInputLink();
        String out = stringWME(il);
        return(out);
    }
    
    public String stringOutputLink() {
        Identifier ol = agent.getInputOutput().getOutputLink();
        String out = stringWME(ol);
        return(out);
    }
    
    public Identifier getInitialState() {
        Set<Wme> allmem = agent.getAllWmesInRete();
        for (Wme w : allmem) {
            Identifier id = w.getIdentifier();
            if (id.toString().equalsIgnoreCase("S1"))
                return(id);
        }
        return(null);
    }
    
    public List<Identifier> getStates() {
        List<Identifier> li = new ArrayList<Identifier>();
        Set<Wme> allmem = agent.getAllWmesInRete();
        for (Wme w : allmem) {
            Identifier id = w.getIdentifier();
            if (id.isGoal()) {
                boolean alreadythere = false;
                for (Identifier icand : li)
                    if (icand == id) alreadythere = true;
                if (alreadythere == false) {
                    li.add(id);
                }
            }
        }
        return(li);
    }
    
    public Set<Wme> getWorkingMemory() {
        return(agent.getAllWmesInRete());
    }
}
