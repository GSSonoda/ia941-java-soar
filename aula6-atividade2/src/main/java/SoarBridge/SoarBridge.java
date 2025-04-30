/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package SoarBridge;

import Simulation.Environment;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
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
import ws3dproxy.model.Leaflet;
import ws3dproxy.model.Bag;
import ws3dproxy.model.Thing;
import ws3dproxy.model.World;
import ws3dproxy.util.Constants;

/**
 *
 * @author Danilo Lucentini and Ricardo Gudwin
 */
public class SoarBridge {

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
    Identifier creatureLeaflets;

    Environment env;
    public Creature c;
    public String input_link_string = "";
    public String output_link_string = "";

    private List<Thing> knownFoods = new ArrayList<>();
    private Set<String> ateFoodName = new HashSet<>();

    private List<Thing> knownJewels = new ArrayList<>();
    private Set<String> gotJewels = new HashSet<>();

    private boolean seenDeliverySpot = false;

    private List<Thing> jewelsToCollect = new ArrayList<>();
    private Leaflet chosenLeaflet = null;

    private boolean seekingBestJewels = false;

    private Map<Long, Boolean> mapLeafletCompleted = new HashMap<>();

    private boolean canCompleteLeaflet;

    private boolean tieOccurred = false;

    private boolean willProcessPlan = false;

    /**
     * Constructor class
     *
     * @param _e Environment
     * @param path Path for Rule Base
     * @param startSOARDebugger set true if you wish the SOAR Debugger to be
     * started
     */
    public SoarBridge(Environment _e, String path, Boolean startSOARDebugger) {
        env = _e;
        c = env.getCreature();
        World word = World.getInstance();

        Map<String, Integer[]> map = new HashMap<>();
        map.put("Red", new Integer[]{1, 0});
        var l1 = new Leaflet(6541L, (HashMap<String, Integer[]>) map, 1200, 0);
        c.addLeaflet(l1);
        mapLeafletCompleted.put(l1.getID(), false);

        Map<String, Integer[]> map1 = new HashMap<>();
        map1.put("White", new Integer[]{1, 0});
        var l2 = new Leaflet(6547L, (HashMap<String, Integer[]>) map1, 1100, 0);
        c.addLeaflet(l2);
        mapLeafletCompleted.put(l2.getID(), false);

        var prox = env.getProxy();

        try {
            CommandUtility.sendNewJewel(0, 150, 90);
            CommandUtility.sendNewJewel(5, 210, 150);
            CommandUtility.sendNewJewel(5, 300, 120);
        } catch (CommandExecException ex) {
            Logger.getLogger(SoarBridge.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            ThreadedAgent tag = ThreadedAgent.create();
            agent = tag.getAgent();
            SoarCommands.source(agent.getInterpreter(), path);
            inputLink = agent.getInputOutput().getInputLink();

            // Initialize entities
            creature = null;

            // Debugger line
            if (startSOARDebugger) {
                agent.openDebugger();
            }
        } catch (Exception e) {
            logger.severe("Error while creating SOAR Kernel");
            e.printStackTrace();
        }
    }

    private Identifier CreateIdWME(Identifier id, String s) {
        SymbolFactory sf = agent.getSymbols();
        Identifier newID = sf.createIdentifier('I');
        agent.getInputOutput().addInputWme(id, sf.createString(s), newID);
        return (newID);
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

    private String getItemType(int categoryType) {
        String itemType = null;

        switch (categoryType) {
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
            case Constants.categoryDeliverySPOT:
                itemType = "DELIVERYSPOT";
                break;
        }
        return itemType;
    }

    /**
     * Create the WMEs at the InputLink of SOAR
     */
    private void prepareInputLink() {
        //SymbolFactory sf = agent.getSymbols();
        Creature c = env.getCreature().updateState();

        inputLink = agent.getInputOutput().getInputLink();
        try {
            if (agent != null) {
                //SimulationCreature creatureParameter = (SimulationCreature)parameter;
                // Initialize Creature Entity
                creature = CreateIdWME(inputLink, "CREATURE");
                // Initialize Creature Memory
                creatureMemory = CreateIdWME(creature, "MEMORY");

                creatureLeaflets = CreateIdWME(creature, "LEAFLETS");

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
                CreateFloatWME(fuel, "VALUE", c.getFuel());
                // Create Visual Sensors
                Identifier visual = CreateIdWME(creatureSensor, "VISUAL");
                List<Thing> thingsList = (List<Thing>) c.getThingsInVision();
                for (Thing t : thingsList) {
                    Identifier entity = CreateIdWME(visual, "ENTITY");
                    CreateFloatWME(entity, "DISTANCE", GetGeometricDistanceToCreature(t.getX1(), t.getY1(), t.getX2(), t.getY2(), c.getPosition().getX(), c.getPosition().getY()));
                    CreateFloatWME(entity, "X", t.getX1());
                    CreateFloatWME(entity, "Y", t.getY1());
                    CreateFloatWME(entity, "X2", t.getX2());
                    CreateFloatWME(entity, "Y2", t.getY2());
                    CreateStringWME(entity, "TYPE", getItemType(t.getCategory()));
                    CreateStringWME(entity, "NAME", t.getName());
                    CreateStringWME(entity, "COLOR", Constants.getColorName(t.getMaterial().getColor()));

                    if (t.getCategory() == Constants.categoryDeliverySPOT) {
                        seenDeliverySpot = true;
                    }
                }

                boolean leafletCompleted = checkLeafletCompleted(c.updateBag());

                if (leafletCompleted) {
                    System.out.println("");
                }
                CreateStringWME(creatureLeaflets, "COMPLETED", leafletCompleted ? "YES" : "NO"); // Note "true" em minúsculas
                CreateStringWME(creatureLeaflets, "SEENDELIVERYSPOT", seenDeliverySpot ? "YES" : "NO"); // Note "true" em minúsculas

                CreateFloatWME(creatureLeaflets, "X", 200);
                CreateFloatWME(creatureLeaflets, "Y", 200);

                updateFoodList(thingsList);
                updateJewelList(thingsList);

                canCompleteLeaflet = canCompleteBestLeafletWithKnownJewels() && !leafletCompleted;
                CreateStringWME(creatureMemory, "CANCOMPLETE", canCompleteLeaflet ? "YES" : "NO");

                if (canCompleteLeaflet) {
                    System.out.println("PODE COMPLETAR");
                    seekingBestJewels = true;

                    // Atualiza as joias que o agente deve coletar
                    for (Thing t : jewelsToCollect) {
                        Identifier entity = CreateIdWME(creatureMemory, "ENTITY");
                        CreateFloatWME(entity, "X", t.getX1());
                        CreateFloatWME(entity, "Y", t.getY1());
                        CreateStringWME(entity, "TYPE", "JEWEL");
                        CreateStringWME(entity, "NAME", t.getName());
                        CreateStringWME(entity, "COLOR", t.getAttributes().getColor());
                        
                    }
                    //AQUI
                    putRequiredJewelsForLeaflet();
                    //System.out.println("joias a coletar: " + jewelsToCollect);
                } else {
                    if (seekingBestJewels && jewelsToCollect.isEmpty()) {
                        seekingBestJewels = false;
                        CreateStringWME(creatureMemory, "COLETAFINALIZADA", "YES");
                        //System.out.println("FINALIZADA");
                    }

                    for (Thing t : knownJewels) {
                        Identifier entity = CreateIdWME(creatureMemory, "ENTITY");
                        CreateFloatWME(entity, "X", t.getX1());
                        CreateFloatWME(entity, "Y", t.getY1());
                        CreateStringWME(entity, "TYPE", "JEWEL");
                        CreateStringWME(entity, "NAME", t.getName());
                        CreateStringWME(entity, "COLOR", t.getAttributes().getColor());
                    }
                }

                for (Thing t : jewelsToCollect) {
                    //System.out.println("jewels to collect: " + jewelsToCollect);
                    boolean found = false;
                    for (var thingL : thingsList) {
                        if (t.getName().equals(thingL.getName())) {
                            found = true;
                            break; // Já encontrou, pode parar
                        }
                    }
                    if (found) {
                        continue; // Não cria se já existe
                    }

                    Identifier entity = CreateIdWME(visual, "ENTITY");
                    CreateFloatWME(entity, "DISTANCE", GetGeometricDistanceToCreature(t.getX1(), t.getY1(), t.getX2(), t.getY2(), c.getPosition().getX(), c.getPosition().getY()));
                    CreateFloatWME(entity, "X", t.getX1());
                    CreateFloatWME(entity, "Y", t.getY1());
                    CreateFloatWME(entity, "X2", t.getX2());
                    CreateFloatWME(entity, "Y2", t.getY2());
                    CreateStringWME(entity, "TYPE", getItemType(t.getCategory()));
                    CreateStringWME(entity, "NAME", t.getName());
                    CreateStringWME(entity, "COLOR", t.getAttributes().getColor());
                }

                CreateFloatWME(creatureMemory, "COUNT", knownFoods.size() + knownJewels.size());
            }
        } catch (Exception e) {
            logger.severe("Error while Preparing Input Link");
            e.printStackTrace();
        }
    }

    private void putRequiredJewelsForLeaflet() {
        var item = (Map<String, Integer>) chosenLeaflet.getWhatToCollect();
        for (var entry : item.entrySet()) {
            //AQUI
            if(entry.getValue()==0){
                continue;
            }
            Identifier entity = CreateIdWME(creatureLeaflets, "ENTITY");
            CreateStringWME(entity, "COLOR", entry.getKey());
            CreateFloatWME(entity, "REQUIRED", entry.getValue());
            //System.out.println("cor a coleetar: " + entry.getKey());
        }
    }

    public Thing getClosestJewelToCollect() {
        var pos = c.getPosition();
        Thing closest = null;
        double minDist = Double.MAX_VALUE;

        for (Thing jewel : jewelsToCollect) {
            double dx = jewel.getX1() - pos.getX();
            double dy = jewel.getY1() - pos.getY();
            double dist = Math.sqrt(dx * dx + dy * dy);

            if (dist < minDist) {
                minDist = dist;
                closest = jewel;
            }
        }

        return closest;
    }

    private boolean checkLeafletCompleted(Bag bag) {
        if (bag == null) {
            System.out.println("BAG NULL");
            return false;
        }

        for (Leaflet l : c.getLeaflets()) {
            HashMap<String, Integer[]> leaflet_map = l.getItems();
            for (Map.Entry<String, Integer[]> entry : leaflet_map.entrySet()) {
                String type = entry.getKey();
                int requiredAmount = entry.getValue()[0]; // total necessário
                int availableInBag = bag.getNumberCrystalPerType(type); // quantidade na bag
        
                if (availableInBag > requiredAmount) {
                    return true;
                }
            }            
        }
        return false;
    }

    public boolean canCompleteBestLeafletWithKnownJewels() {
        jewelsToCollect.clear();
        chosenLeaflet = null;

        if (knownJewels == null || knownJewels.isEmpty() || c == null || c.getLeaflets() == null) {
            return false;
        }

        Map<String, List<Thing>> jewelsByColor = new HashMap<>();
        for (Thing jewel : knownJewels) {
            if (jewel != null && jewel.getAttributes() != null) {
                String color = jewel.getAttributes().getColor();
                if (color != null) {
                    jewelsByColor.computeIfAbsent(color, k -> new ArrayList<>()).add(jewel);
                }
            }
        }

        int bestScore = -1;
        List<Thing> bestJewels = null;

        for (Leaflet leaflet : c.getLeaflets()) {
            if (leaflet == null || leaflet.getItems() == null) {
                continue;
            }

            if (isLeafletComplete(leaflet)) {
                continue;
            }

            Map<String, Integer> missing = leaflet.getWhatToCollect();
            if (missing == null || missing.isEmpty()) {
                continue;
            }

            List<Thing> tempJewels = new ArrayList<>();
            boolean canComplete = true;

            for (Map.Entry<String, Integer> entry : missing.entrySet()) {
                String color = entry.getKey();
                int required = entry.getValue();

                if (required <= 0) {
                    continue;
                }

                List<Thing> available = jewelsByColor.getOrDefault(color, Collections.emptyList());

                available.sort(Comparator.comparingDouble(jewel -> GetGeometricDistanceToCreature(jewel.getX1(), jewel.getY1(), jewel.getX2(), jewel.getY2(), c.getPosition().getX(), c.getPosition().getY())));

                if (available.size() < required) {
                    canComplete = false;
                    break;
                }

                int count = Math.min(required, available.size());
                for (int i = 0; i < count; i++) {
                    tempJewels.add(available.get(i));
                }
            }

            if (canComplete && !tempJewels.isEmpty()) {
                int points = leaflet.getPayment();
                if (points > bestScore) {
                    bestScore = points;
                    bestJewels = new ArrayList<>(tempJewels);
                    chosenLeaflet = leaflet;
                    //System.out.println("voluntario: " + chosenLeaflet);
                }
            }
        }

        if (bestJewels != null && !bestJewels.isEmpty()) {
            jewelsToCollect.addAll(bestJewels);
            //System.out.println("escolhido: " + chosenLeaflet);
            return true;
        }

        return false;
    }

    private void updateJewelList(List<Thing> thingsList) {
        for (Thing t : thingsList) {
            if (getItemType(t.getCategory()) != "JEWEL" || gotJewels.contains(t.getName())) {
                continue;
            }

            Set<String> neededJewelColors = new HashSet<>();
            for (Leaflet l : c.getLeaflets()) {
                if (isLeafletComplete(l)) {
                    continue;
                }

                Map<String, Integer> map = (Map<String, Integer>) l.getWhatToCollect();
                for (var jewelColor : map.keySet()) {
                    neededJewelColors.add(jewelColor);
                }
            }

            boolean found = false;
            for (Thing known : knownJewels) {
                if (known.getName().equals(t.getName())) {
                    found = true;
                    break;
                }
            }
            if (!found && neededJewelColors.contains(t.getAttributes().getColor())) {
                knownJewels.add(t);
            }
        }

    }

    private void updateFoodList(List<Thing> thingsList) {

        // Verifica comidas visíveis
        for (Thing t : thingsList) {
            if (getItemType(t.getCategory()) != "FOOD" || ateFoodName.contains(t.getName())) {

                continue;
            }

            boolean found = false;
            for (Thing known : knownFoods) {
                if (known.getName().equals(t.getName())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                knownFoods.add(t);
            }
        }

        for (Thing t : knownFoods) {
            Identifier entity = CreateIdWME(creatureMemory, "ENTITY");
            CreateFloatWME(entity, "X", t.getX1());
            CreateFloatWME(entity, "Y", t.getY1());
            CreateStringWME(entity, "TYPE", "FOOD");
            CreateStringWME(entity, "NAME", t.getName());
        }
    }

    private double GetGeometricDistanceToCreature(double x1, double y1, double x2, double y2, double xCreature, double yCreature) {
        float squared_dist = 0.0f;
        double maxX = Math.max(x1, x2);
        double minX = Math.min(x1, x2);
        double maxY = Math.max(y1, y2);
        double minY = Math.min(y1, y2);

        if (xCreature > maxX) {
            squared_dist += (xCreature - maxX) * (xCreature - maxX);
        } else if (xCreature < minX) {
            squared_dist += (minX - xCreature) * (minX - xCreature);
        }

        if (yCreature > maxY) {
            squared_dist += (yCreature - maxY) * (yCreature - maxY);
        } else if (yCreature < minY) {
            squared_dist += (minY - yCreature) * (minY - yCreature);
        }

        return Math.sqrt(squared_dist);
    }

    private double getDistanceToJewel(double xJewel, double yJewel, double xCreature, double yCreature) {
        double dx = xCreature - xJewel;
        double dy = yCreature - yJewel;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private void resetSimulation() {
        agent.initialize();
    }

    /**
     * Run SOAR until HALT
     */
    private void runSOAR() {
        agent.runForever();
    }

    private int stepSOAR() {
        agent.runFor(1, RunType.PHASES);
        Phase ph = agent.getCurrentPhase();
        if (ph.equals(Phase.INPUT)) {
            return (0);
        } else if (ph.equals(Phase.PROPOSE)) {
            return (1);
        } else if (ph.equals(Phase.DECISION)) {
            return (2);
        } else if (ph.equals(Phase.APPLY)) {
            return (3);
        } else if (ph.equals(Phase.OUTPUT)) {
            if (agent.getReasonForStop() == null) {
                return (4);
            } else {
                return (5);
            }
        } else {
            return (6);
        }
    }

    private String GetParameterValue(String par) {
        List<Wme> Commands = Wmes.matcher(agent).filter(agent.getInputOutput().getOutputLink());
        List<Wme> Parameters = Wmes.matcher(agent).filter(Commands.get(0));
        String parvalue = "";
        for (Wme w : Parameters) {
            String att = w.getAttribute().toString();
            if (w.getAttribute().toString().equals(par)) {
                parvalue = w.getValue().toString();
            }
        }
        return (parvalue);
    }

    private List<Command> GetParameterValuePlan(String par) {
        List<Command> commandList = new ArrayList<>();

        List<Wme> commands = Wmes.matcher(agent).filter(agent.getInputOutput().getOutputLink());
        if (commands.isEmpty()) {
            return commandList;
        }

        // Procurar o WME que tenha atributo "PLAN"
        Wme planWme = null;
        for (Wme wme : commands) {
            if ("PLAN".equals(wme.getAttribute().toString())) {
                planWme = wme;
                break;
            }
        }

        if (planWme == null) {
            return commandList; // Não achou um plano
        }

        List<Wme> parameters = Wmes.matcher(agent).filter(planWme);

        for (Wme step : parameters) {
            List<Wme> attrs = Wmes.matcher(agent).filter(step);

            if (attrs.isEmpty()) {
                continue;
            }

            String commandType = attrs.get(0).getValue().toString();

            if (commandType.equals("GET")) {
                String jewelName = attrs.get(1).getValue().toString();
                CommandGet getCmd = new CommandGet();
                getCmd.setThingName(jewelName);
                Command command = new Command(Command.CommandType.GET);
                command.setCommandArgument(getCmd);
                commandList.add(command);
            }

            if (commandType.equals("MOVE")) {
                double x = attrs.get(1).getValue().asDouble().getValue();
                double y = attrs.get(2).getValue().asDouble().getValue();
                double vel = 1;
                double velL = 1;
                double velR = 1;
                CommandMove moveCmd = new CommandMove();
                moveCmd.setX((float) x);
                moveCmd.setY((float) y);
                moveCmd.setLinearVelocity((float) vel);
                moveCmd.setLeftVelocity((float) velL);
                moveCmd.setRightVelocity((float) velR);
                Command command = new Command(Command.CommandType.MOVE);
                command.setCommandArgument(moveCmd);
                commandList.add(command);
            }
        }

        Set<Command> set = new LinkedHashSet<>(commandList);
        commandList = new ArrayList<>(set);
        return commandList;
    }

    /**
     * Process the OutputLink given by SOAR and return a list of commands to
     * WS3D
     *
     * @return A List of SOAR Commands
     */
    private ArrayList<Command> processOutputLink() {
        ArrayList<Command> commandList = new ArrayList<Command>();

        try {
            if (agent != null) {
                List<Wme> Commands = Wmes.matcher(agent).filter(agent.getInputOutput().getOutputLink());

                willProcessPlan = false;

                for (Wme com : Commands) {
                    String name = com.getAttribute().asString().getValue();
                    Command.CommandType commandType = Enum.valueOf(Command.CommandType.class, name);
                    Command command = null;

                    switch (commandType) {
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
                            CommandMove commandMove = (CommandMove) command.getCommandArgument();
                            if (commandMove != null) {
                                if (rightVelocity != null) {
                                    commandMove.setRightVelocity(rightVelocity);
                                }
                                if (leftVelocity != null) {
                                    commandMove.setLeftVelocity(leftVelocity);
                                }
                                if (linearVelocity != null) {
                                    commandMove.setLinearVelocity(linearVelocity);
                                }
                                if (xPosition != null) {
                                    commandMove.setX(xPosition);
                                }
                                if (yPosition != null) {
                                    commandMove.setY(yPosition);
                                }
                                commandList.add(command);
                            } else {
                                logger.severe("Error processing MOVE command");
                            }
                            break;

                        case GET:
                            String thingNameToGet = null;
                            command = new Command(Command.CommandType.GET);
                            CommandGet commandGet = (CommandGet) command.getCommandArgument();
                            if (commandGet != null) {
                                thingNameToGet = GetParameterValue("Name");
                                if (thingNameToGet != null) {
                                    commandGet.setThingName(thingNameToGet);
                                }
                                commandList.add(command);
                            }
                            break;

                        case EAT:
                            String thingNameToEat = null;
                            command = new Command(Command.CommandType.EAT);
                            CommandEat commandEat = (CommandEat) command.getCommandArgument();
                            if (commandEat != null) {
                                thingNameToEat = GetParameterValue("Name");
                                if (thingNameToEat != null) {
                                    commandEat.setThingName(thingNameToEat);
                                }
                                commandList.add(command);
                            }
                            break;

                        case DELIVER:

                            command = new Command(Command.CommandType.DELIVER);
                            CommandDeliver commandDeliver = (CommandDeliver) command.getCommandArgument();
                            if (commandDeliver != null) {
                                commandList.add(command);
                            }
                            break;
                        case TIE:
                            tieOccurred = true;

                            break;
                        case PLAN:
                            command = new Command(Command.CommandType.PLAN);
                            CommandPlan commandPlan = (CommandPlan) command.getCommandArgument();
                            if (commandPlan != null) {
                                List<Command> step = GetParameterValuePlan("PLAN");
                                Collections.reverse(step);
                                String plano = "";
                                for (var item : step) {
                                    commandList.add(item);
                                    plano += item.getCommandType().toString() + " ";
                                }
                                
                                System.out.println("plano traçado: " + plano);
//                                if(!step.isEmpty()){
//                                    commandList.add(step.get(0));
//                                }

                            }

                            willProcessPlan = true;

                            break;
                        default:
                            System.out.println("");

                            break;
                    }
                }
            }
        } catch (Exception e) {
            logger.severe("Error while processing commands");
            e.printStackTrace();
        }
        int size = commandList.size();
        if (size > 0) {
            tieOccurred = false;
        }
        return ((commandList.size() > 0) ? commandList : null);
    }

    /**
     * Perform a complete SOAR step
     *
     * @throws ws3dproxy.CommandExecException
     */
    public void step() throws CommandExecException {
        if (phase != -1) {
            finish_msteps();
        }
        resetSimulation();
        c.updateState();
        prepareInputLink();
        input_link_string = stringInputLink();
        //printInputWMEs();
        runSOAR();
        output_link_string = stringOutputLink();
        //printOutputWMEs();
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

    public int phase = -1;

    public void mstep() throws CommandExecException {
        if (phase == -1) {
            prepare_mstep();
        }
        phase = stepSOAR();
        if (phase == 5) {
            post_mstep();
            phase = -1;
        }
    }

    public void finish_msteps() throws CommandExecException {
        while (phase != -1) {
            mstep();
        }
    }

    public void post_mstep() throws CommandExecException {
        output_link_string = stringOutputLink();
        //printOutputWMEs();
        List<Command> commandList = processOutputLink();
        processCommands(commandList);
        //resetSimulation();
    }

    private void processCommands(List<Command> commandList) throws CommandExecException {

        if (tieOccurred) {
            return;
        }
        if (commandList != null) {
            for (Command command : commandList) {
                switch (command.getCommandType()) {
                    case MOVE:
                        processMoveCommand((CommandMove) command.getCommandArgument());
                        break;

                    case GET:
                        processGetCommand((CommandGet) command.getCommandArgument());
                        break;

                    case EAT:
                        processEatCommand((CommandEat) command.getCommandArgument());
                        break;
                    case DELIVER:
                        processDeliverCommand((CommandDeliver) command.getCommandArgument());

                    default:
                        break;
                }
            }
        } else {
        }
    }

    /**
     * Send Move Command to World Server
     *
     * @param soarCommandMove Soar Move Command Structure
     */
    private void processMoveCommand(CommandMove soarCommandMove) throws CommandExecException {
        if (soarCommandMove != null) {
            if (soarCommandMove.getX() != null && soarCommandMove.getY() != null) {
                CommandUtility.sendGoTo("0", soarCommandMove.getRightVelocity(), soarCommandMove.getLeftVelocity(), soarCommandMove.getX(), soarCommandMove.getY());
                if (willProcessPlan) {
                    waitUntilCloseEnough(soarCommandMove.getX(), soarCommandMove.getY());
                }
            } else {
                CommandUtility.sendSetTurn("0", soarCommandMove.getLinearVelocity(), soarCommandMove.getRightVelocity(), soarCommandMove.getLeftVelocity());
            }
        } else {
            logger.severe("Error processing processMoveCommand");
        }
    }

    private void waitUntilCloseEnough(float targetX, float targetY) {
        while (true) {
            c = c.updateState();
            double currentX = c.getPosition().getX();
            double currentY = c.getPosition().getY();

            // Calcula a distância até o alvo
            double distance = getDistanceToJewel(targetX, targetY, currentX, currentY);

            if (distance <= 30.0) {
                break; // Sai do loop se estiver perto o suficiente
            }

            try {
                Thread.sleep(100); // Espera 100 ms antes de checar de novo
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Send Get Command to World Server
     *
     * @param soarCommandGet Soar Get Command Structure
     */
    private void processGetCommand(CommandGet soarCommandGet) throws CommandExecException {
        if (soarCommandGet != null) {
            c = env.getCreature().updateState();

            var leaflets = c.getLeaflets();

            String color = null;
            for (Thing jewel : c.getThingsInVision()) {
                if (jewel.getName().equals(soarCommandGet.getThingName())) {
                    color = jewel.getAttributes().getColor();
                    break;
                }
            }

            if (color == null) {
                return;
            }

            if (leaflets != null) {
                for (var leaflet : leaflets) {

                    var itemsMap = leaflet.getItems();

                    if (itemsMap.containsKey(color)) {
                        Integer[] counts = itemsMap.get(color);

                        if (counts[0] == counts[1]) {
                            continue;
                        }

                        counts[1] += 1;
                        itemsMap.put(color, counts);

                        leaflet.setItems(itemsMap);
                        c.updateLeaflet(leaflet.getID(), leaflet.getItems(), leaflet.getSituation());
                    }
                }
            }

            c.putInSack(soarCommandGet.getThingName());
            c = c.updateState();
            knownJewels.removeIf(thing -> thing.getName().equals(soarCommandGet.getThingName()));
            jewelsToCollect.removeIf(thing -> thing.getName().equals(soarCommandGet.getThingName()));
            gotJewels.add(soarCommandGet.getThingName());

        } else {
            logger.severe("Error processing processMoveCommand");
        }
    }

    /**
     * Send Eat Command to World Server
     *
     * @param soarCommandEat Soar Eat Command Structure
     */
    private void processEatCommand(CommandEat soarCommandEat) throws CommandExecException {
        if (soarCommandEat != null) {
            c.eatIt(soarCommandEat.getThingName());

            knownFoods.removeIf(thing -> thing.getName().equals(soarCommandEat.getThingName()));
            ateFoodName.add(soarCommandEat.getThingName());

        } else {
            logger.severe("Error processing processMoveCommand");
        }
    }

    private void processDeliverCommand(CommandDeliver commandDeliver) {
        if (commandDeliver != null) {
            for (Leaflet l : c.getLeaflets()) {
                // Verifica apenas pelo flag isCompleted e se não foi entregue
                try {
                    c.deliverLeaflet(String.valueOf(l.getID()));
                    if (mapLeafletCompleted.get(l.getID()) != null) {
                        mapLeafletCompleted.put(l.getID(), true);
                        //System.out.println("entrgou leaflet meta" + l);
                    }
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(SoarBridge.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    c = c.updateState();

                } catch (CommandExecException ex) {
                    Logger.getLogger(SoarBridge.class.getName()).log(Level.SEVERE, null, ex);
                    continue;
                }
            }
        } else {
            logger.severe("Error processing deliver command");
        }
    }

    private boolean isLeafletComplete(Leaflet leaflet) {
        if (leaflet == null) {
            return false;
        }

        Map<String, Integer[]> items = leaflet.getItems();
        if (items == null || items.isEmpty()) {
            return false;
        }

        for (Map.Entry<String, Integer[]> entry : items.entrySet()) {
            Integer[] quantities = entry.getValue();
            if (quantities == null || quantities.length < 2) {
                return false;
            }

            int required = quantities[0];
            int collected = quantities[1];

            if (collected < required) {
                return false;
            }
        }

        return true; // Todos os itens estão completos
    }

    /**
     * Try Parse a Float Element
     *
     * @param value Float Value
     * @return The Float Value or null otherwise
     */
    private Float tryParseFloat(String value) {
        Float returnValue = null;

        try {
            returnValue = Float.parseFloat(value);
        } catch (Exception ex) {
            returnValue = null;
        }

        return returnValue;
    }

    public void printWME(Identifier id) {
        printWME(id, 0);

    }

    public void printWME(Identifier id, int level) {
        Iterator<Wme> It = id.getWmes();
        while (It.hasNext()) {
            Wme wme = It.next();
            Identifier idd = wme.getIdentifier();
            Symbol a = wme.getAttribute();
            Symbol v = wme.getValue();
            Identifier testv = v.asIdentifier();
            for (int i = 0; i < level; i++) {
                System.out.print("   ");
            }
            if (testv != null) {
                System.out.print("(" + idd.toString() + "," + a.toString() + "," + v.toString() + ")\n");
                printWME(testv, level + 1);
            } else {
                System.out.print("(" + idd.toString() + "," + a.toString() + "," + v.toString() + ")\n");
            }
        }
    }

    public void printInputWMEs() {
        Identifier il = agent.getInputOutput().getInputLink();
        System.out.println("Input --->");
        printWME(il);
    }

    public void printOutputWMEs() {
        Identifier ol = agent.getInputOutput().getOutputLink();
        System.out.println("Output --->");
        printWME(ol);
    }

    public String stringWME(Identifier id) {
        String out = stringWME(id, 0);
        return (out);
    }

    public String stringWME(Identifier id, int level) {
        String out = "";
        Iterator<Wme> It = id.getWmes();
        while (It.hasNext()) {
            Wme wme = It.next();
            Identifier idd = wme.getIdentifier();
            Symbol a = wme.getAttribute();
            Symbol v = wme.getValue();
            Identifier testv = v.asIdentifier();
            for (int i = 0; i < level; i++) {
                out += "   ";
            }
            if (testv != null) {
                out += "(" + idd.toString() + "," + a.toString() + "," + v.toString() + ")\n";
                out += stringWME(testv, level + 1);
            } else {
                out += "(" + idd.toString() + "," + a.toString() + "," + v.toString() + ")\n";
            }
        }
        return (out);
    }

    public String stringInputLink() {
        Identifier il = agent.getInputOutput().getInputLink();
        String out = stringWME(il);
        return (out);
    }

    public String stringOutputLink() {
        Identifier ol = agent.getInputOutput().getOutputLink();
        String out = stringWME(ol);
        return (out);
    }

    public Identifier getInitialState() {
        Set<Wme> allmem = agent.getAllWmesInRete();
        for (Wme w : allmem) {
            Identifier id = w.getIdentifier();
            if (id.toString().equalsIgnoreCase("S1")) {
                return (id);
            }
        }
        return (null);
    }

    public List<Identifier> getStates() {
        List<Identifier> li = new ArrayList<Identifier>();
        Set<Wme> allmem = agent.getAllWmesInRete();
        for (Wme w : allmem) {
            Identifier id = w.getIdentifier();
            if (id.isGoal()) {
                boolean alreadythere = false;
                for (Identifier icand : li) {
                    if (icand == id) {
                        alreadythere = true;
                    }
                }
                if (alreadythere == false) {
                    li.add(id);
                }
            }
        }
        return (li);
    }

    public Set<Wme> getWorkingMemory() {
        return (agent.getAllWmesInRete());
    }

}
