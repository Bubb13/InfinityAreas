
package com.github.bubb13.infinityareas.game.resource;

import com.github.bubb13.infinityareas.GlobalState;
import com.github.bubb13.infinityareas.game.Game;
import com.github.bubb13.infinityareas.gui.editor.GenericPolygon;
import com.github.bubb13.infinityareas.misc.ImageAndGraphics;
import com.github.bubb13.infinityareas.misc.IntPoint;
import com.github.bubb13.infinityareas.misc.IntPointImpl;
import com.github.bubb13.infinityareas.misc.OrderedInstanceSet;
import com.github.bubb13.infinityareas.misc.ReferenceHolder;
import com.github.bubb13.infinityareas.misc.ReferenceTrackable;
import com.github.bubb13.infinityareas.misc.ReferenceTracker;
import com.github.bubb13.infinityareas.misc.SimpleLinkedList;
import com.github.bubb13.infinityareas.misc.TaskTracker;
import com.github.bubb13.infinityareas.misc.TaskTrackerI;
import com.github.bubb13.infinityareas.misc.TrackedTask;
import com.github.bubb13.infinityareas.misc.TrackingOrderedInstanceSet;
import com.github.bubb13.infinityareas.util.BufferUtil;
import com.github.bubb13.infinityareas.util.MiscUtil;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Stack;

public class Area
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final Game.ResourceSource source;
    private final Stack<Integer> bufferMarks = new Stack<>();

    private ByteBuffer buffer;
    private WED wed;

    private byte[] unimplemented1;
    private final TrackingOrderedInstanceSet<Actor> actors = new TrackingOrderedInstanceSet<>();
    private final TrackingOrderedInstanceSet<Region> regions = new TrackingOrderedInstanceSet<>();
    private final TrackingOrderedInstanceSet<SpawnPoint> spawnPoints = new TrackingOrderedInstanceSet<>();
    private final TrackingOrderedInstanceSet<Entrance> entrances = new TrackingOrderedInstanceSet<>();
    private final TrackingOrderedInstanceSet<Container> containers = new TrackingOrderedInstanceSet<>();
    private final TrackingOrderedInstanceSet<Ambient> ambients = new TrackingOrderedInstanceSet<>();
    private final TrackingOrderedInstanceSet<Variable> variables = new TrackingOrderedInstanceSet<>();
    private String areaScript;
    private ExploredBitmask exploredBitmask;
    private final TrackingOrderedInstanceSet<Door> doors = new TrackingOrderedInstanceSet<>();
    private final TrackingOrderedInstanceSet<Animation> animations = new TrackingOrderedInstanceSet<>();
    private final TrackingOrderedInstanceSet<TiledObject> tiledObjects = new TrackingOrderedInstanceSet<>();
    private SongEntries songEntries;
    private RestInterruptions restInterruptions;
    private int specialPSTField;
    private final TrackingOrderedInstanceSet<MapNote> mapNotes = new TrackingOrderedInstanceSet<>();
    private final TrackingOrderedInstanceSet<ProjectileTrap> projectileTraps = new TrackingOrderedInstanceSet<>();
    private byte[] unimplemented2;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public Area(final Game.ResourceSource source)
    {
        this.source = source;
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public Game.ResourceSource getSource()
    {
        return source;
    }

    public void load(final TaskTrackerI tracker) throws Exception
    {
        tracker.subtask(this::loadInternal);
    }

    public void load() throws Exception
    {
        loadInternal(TaskTracker.DUMMY);
    }

    public TrackedTask<Void> loadTask()
    {
        return new TrackedTask<>()
        {
            @Override
            protected Void doTask(final TaskTrackerI tracker) throws Exception
            {
                subtask(Area.this::loadInternal);
                return null;
            }
        };
    }

    public void save(final TaskTrackerI tracker, final Path path) throws Exception
    {
        final SaveAreaState saveAreaState = new SaveAreaState(path);
        tracker.subtask(saveAreaState::save);
    }

    public void save(final Path path) throws Exception
    {
        final SaveAreaState saveAreaState = new SaveAreaState(path);
        saveAreaState.save(TaskTracker.DUMMY);
    }

    public TrackedTask<Void> saveTask(final Path path)
    {
        return new TrackedTask<>()
        {
            @Override
            protected Void doTask(final TaskTrackerI tracker) throws Exception
            {
                final SaveAreaState saveAreaState = new SaveAreaState(path);
                subtask(saveAreaState::save);
                return null;
            }
        };
    }

    public int getOverlayCount()
    {
        return wed.getOverlays().size();
    }

    public TrackedTask<BufferedImage> renderOverlaysTask(final int... overlayIndexes)
    {
        return wed.renderOverlaysTask(overlayIndexes);
    }

    public BufferedImage renderOverlays(final TaskTrackerI tracker, final int... overlayIndexes) throws Exception
    {
        final WED.Graphics wedGraphics = wed.newGraphics();
        wedGraphics.renderOverlays(tracker, overlayIndexes);
        return wedGraphics.getImage();
    }

    public Iterable<Actor> actors()
    {
        return MiscUtil.readOnlyIterable(actors);
    }

    public void addRegion(final Region region)
    {
        regions.addTail(region);
    }

    public Iterable<Region> regions()
    {
        return MiscUtil.readOnlyIterable(regions);
    }

    public Graphics newGraphics(final ImageAndGraphics imageAndGraphics)
    {
        return new Graphics(imageAndGraphics);
    }

    public Graphics newGraphics()
    {
        final WED.Overlay baseOverlay = wed.getOverlays().get(0);
        final BufferedImage image = new BufferedImage(
            baseOverlay.getWidthInTiles() * 64,
            baseOverlay.getHeightInTiles() * 64,
            BufferedImage.TYPE_INT_ARGB
        );
        return new Graphics(new ImageAndGraphics(image, image.createGraphics()));
    }

    public WED getWed()
    {
        return wed;
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void position(final int pos)
    {
        buffer.position(pos);
    }

    //-------------------//
    // START Loading ARE //
    //-------------------//

    private void loadInternal(final TaskTrackerI tracker) throws Exception
    {
        buffer = source.demandFileData();

        tracker.updateProgress(0, 100);
        tracker.updateMessage("Processing area ...");

        position(0x0);

        final String signature = BufferUtil.readUTF8(buffer, 4);
        if (!signature.equals("AREA"))
        {
            throw new IllegalStateException("Invalid ARE signature: \"" + signature + "\"");
        }

        final String version = BufferUtil.readUTF8(buffer, 4);
        if (version.equals("V1.0"))
        {
            parse_V1_0(tracker);
        }
        else if (version.equals("V9.1"))
        {
            parse_V9_1(tracker);
        }
        else
        {
            throw new IllegalStateException("Invalid ARE version: \"" + version + "\"");
        }
    }

    private void parse_V1_0(final TaskTrackerI tracker) throws Exception
    {
        loadWED(tracker);

        unimplemented1 = new byte[0x44]; buffer.get(unimplemented1);
        final int actorsOffset = buffer.getInt();
        final short numActors = buffer.getShort();
        final short numRegions = buffer.getShort();
        final int regionsOffset = buffer.getInt();
        final int spawnPointsOffset = buffer.getInt();
        final int numSpawnPoints = buffer.getInt();
        final int entrancesOffset = buffer.getInt();
        final int numEntrances = buffer.getInt();
        final int containersOffset = buffer.getInt();
        final short numContainers = buffer.getShort();
        @SuppressWarnings("unused") final short numItems = buffer.getShort();
        final int itemsOffset = buffer.getInt();
        final int verticesOffset = buffer.getInt();
        @SuppressWarnings("unused") final short numVertices = buffer.getShort();
        final short numAmbients = buffer.getShort();
        final int ambientsOffset = buffer.getInt();
        final int variablesOffset = buffer.getInt();
        final short numVariables = buffer.getShort();
        @SuppressWarnings("unused") final short numObjectFlags = buffer.getShort();
        @SuppressWarnings("unused") final int objectFlagsOffset = buffer.getInt();
        areaScript = BufferUtil.readLUTF8(buffer, 8);
        final int exploredBitmaskSize = buffer.getInt();
        final int exploredBitmaskOffset = buffer.getInt();
        final int numDoors = buffer.getInt();
        final int doorsOffset = buffer.getInt();
        final int numAnimations = buffer.getInt();
        final int animationsOffset = buffer.getInt();
        final int numTiledObjects = buffer.getInt();
        final int tiledObjectsOffset = buffer.getInt();
        final int songEntriesOffset = buffer.getInt();
        final int restInterruptionsOffset = buffer.getInt();

        int unimplementedSize;
        if (GlobalState.getGame().getEngineType() == Game.Type.PST)
        {
            specialPSTField = buffer.getInt();
            unimplementedSize = 0x44;
        }
        else
        {
            unimplementedSize = 0x48;
        }

        final boolean hasMapNotes = hasMapNotes();
        int mapNotesOffset = 0;
        int numMapNotes = 0;
        if (hasMapNotes)
        {
            mapNotesOffset = buffer.getInt();
            numMapNotes = buffer.getInt();
        }
        else
        {
            unimplementedSize += 8;
        }

        final boolean hasProjectileTraps = hasProjectileTraps();
        int projectileTrapsOffset = 0;
        int numProjectileTraps = 0;
        if (hasProjectileTraps)
        {
            projectileTrapsOffset = buffer.getInt();
            numProjectileTraps = buffer.getInt();
        }
        else
        {
            unimplementedSize += 8;
        }

        unimplemented2 = new byte[unimplementedSize]; buffer.get(unimplemented2);

        parseActors(tracker, actorsOffset, numActors);
        parseRegions(tracker, regionsOffset, numRegions, verticesOffset);
        parseSpawnPoints(spawnPointsOffset, numSpawnPoints);
        parseEntrances(entrancesOffset, numEntrances);
        parseContainers(itemsOffset, verticesOffset, containersOffset, numContainers);
        parseAmbients(ambientsOffset, numAmbients);
        parseVariables(variablesOffset, numVariables);
        parseExploredBitmask(exploredBitmaskOffset, exploredBitmaskSize);
        parseDoors(verticesOffset, doorsOffset, numDoors);
        parseAnimations(animationsOffset, numAnimations);
        parseTiledObjects(tiledObjectsOffset, numTiledObjects);
        parseSongEntries(songEntriesOffset);
        parseRestInterruptions(restInterruptionsOffset);

        if (hasMapNotes)
        {
            parseMapNotes(mapNotesOffset, numMapNotes);
        }

        if (hasProjectileTraps && numProjectileTraps > 0)
        {
            parseProjectileTraps(projectileTrapsOffset, numProjectileTraps);
        }
    }

    private boolean hasMapNotes()
    {
        return switch (GlobalState.getGame().getEngineType())
        {
            case PST, SOA, TOB, BGEE, BG2EE, IWDEE, PSTEE -> true;
            default -> false;
        };
    }

    private boolean hasProjectileTraps()
    {
        return switch (GlobalState.getGame().getEngineType())
        {
            case SOA, TOB, BGEE, BG2EE, IWDEE, PSTEE -> true;
            default -> false;
        };
    }

    private void parseActors(
        final TaskTrackerI tracker, final int actorsOffset, final short numActors)
    {
        tracker.updateMessage("Processing actors ...");
        tracker.updateProgress(0, numActors);

        position(actorsOffset);

        for (int i = 0; i < numActors; ++i)
        {
            final Actor actor = new Actor();

            actor.setName(BufferUtil.readLUTF8(buffer, 32));
            actor.setX(buffer.getShort());
            actor.setY(buffer.getShort());

            final byte[] unimplemented1 = new byte[0x10]; buffer.get(unimplemented1);
            actor.setUnimplemented1(unimplemented1);

            actor.setOrientation(buffer.getShort());

            final byte[] unimplemented2 = new byte[0x4A]; buffer.get(unimplemented2);
            actor.setUnimplemented2(unimplemented2);

            final String creatureResref = BufferUtil.readLUTF8(buffer, 8);
            actor.setCreatureResref(creatureResref);

            final int creatureOffset = buffer.getInt();
            @SuppressWarnings("unused") final int creatureSize = buffer.getInt();

            if (creatureResref.isEmpty() || creatureResref.startsWith("*"))
            {
                actor.setCreature(parseCreature(creatureOffset));
            }

            final byte[] unimplemented3 = new byte[0x80]; buffer.get(unimplemented3);
            actor.setUnimplemented3(unimplemented3);

            actors.addTail(actor);
        }
    }

    private Creature parseCreature(final int creatureOffset)
    {
        throw new UnsupportedOperationException("Cannot handle embedded creatures at this time");
    }

    private void parseRegions(
        final TaskTrackerI tracker, final int regionsOffset, final short numRegions, final int verticesOffset)
    {
        tracker.updateMessage("Processing regions ...");
        tracker.updateProgress(0, numRegions);

        position(regionsOffset);
        for (int i = 0; i < numRegions; ++i, tracker.updateProgress(i, numRegions))
        {
            regions.addTail(parseRegion(verticesOffset));
        }
    }

    private Region parseRegion(final int verticesOffset)
    {
        final Region region = new Region(NO_INIT.DUMMY);

        region.setName(BufferUtil.readLUTF8(buffer, 32));
        region.setType(buffer.getShort());
        final short boundingBoxLeft = buffer.getShort();
        final short boundingBoxTop = buffer.getShort();
        final short boundingBoxRight = buffer.getShort();
        final short boundingBoxBottom = buffer.getShort();
        final short numVertices = buffer.getShort();
        final int firstVertexIndex = buffer.getInt();

        final GenericPolygon polygon = new GenericPolygon(
            boundingBoxLeft, boundingBoxRight,
            boundingBoxTop, boundingBoxBottom
        );
        parseVertices(polygon, verticesOffset, firstVertexIndex, numVertices);
        region.setPolygon(polygon);

        region.setTriggerValue(buffer.getInt());
        region.setCursorIndex(buffer.getInt());
        region.setDestAreaResref(BufferUtil.readLUTF8(buffer, 8));
        region.setEntranceNameInDestArea(BufferUtil.readLUTF8(buffer, 32));
        region.setFlags(buffer.getInt());
        region.setInfoStrref(buffer.getInt());
        region.setTrapDetectionDifficulty(buffer.getShort());
        region.setTrapDisarmDifficulty(buffer.getShort());
        region.setbTrapped(buffer.getShort());
        region.setbTrapDetected(buffer.getShort());
        final int trapLaunchPointX = MiscUtil.toUnsignedShort(buffer.getShort());
        final int trapLaunchPointY = MiscUtil.toUnsignedShort(buffer.getShort());
        region.setTrapLaunchPoint(new IntPointImpl(trapLaunchPointX, trapLaunchPointY));
        region.setKeyResref(BufferUtil.readLUTF8(buffer, 8));
        region.setScriptResref(BufferUtil.readLUTF8(buffer, 8));
        region.setActivationPointX(buffer.getShort());
        region.setActivationPointY(buffer.getShort());
        final byte[] unknown = new byte[0x3C]; buffer.get(unknown);
        region.setUnknown(unknown);

        return region;
    }

    private void parseSpawnPoints(final int spawnPointsOffset, final int numSpawnPoints)
    {
        position(spawnPointsOffset);

        for (int i = 0; i < numSpawnPoints; ++i)
        {
            final SpawnPoint spawnPoint = new SpawnPoint();

            final byte[] unimplemented = new byte[0xC8]; buffer.get(unimplemented);
            spawnPoint.setUnimplemented(unimplemented);

            spawnPoints.addTail(spawnPoint);
        }
    }

    private void parseEntrances(final int entrancesOffset, final int numEntrances)
    {
        position(entrancesOffset);

        for (int i = 0; i < numEntrances; ++i)
        {
            final Entrance entrance = new Entrance();

            final byte[] unimplemented = new byte[0x68]; buffer.get(unimplemented);
            entrance.setUnimplemented(unimplemented);

            entrances.addTail(entrance);
        }
    }

    private void parseContainers(
        final int itemsOffset, final int verticesOffset, final int containersOffset, final short numContainers)
    {
        position(containersOffset);

        for (int i = 0; i < numContainers; ++i)
        {
            final Container container = new Container();

            final byte[] unimplemented1 = new byte[0x38]; buffer.get(unimplemented1);
            container.setUnimplemented1(unimplemented1);

            final short boundingBoxLeft = buffer.getShort();
            final short boundingBoxTop = buffer.getShort();
            final short boundingBoxRight = buffer.getShort();
            final short boundingBoxBottom = buffer.getShort();

            final int firstItemIndex = buffer.getInt();
            final int numItems = buffer.getInt();
            final TrackingOrderedInstanceSet<Item> items = new TrackingOrderedInstanceSet<>();
            parseItems(items, itemsOffset, firstItemIndex, numItems);
            container.setItems(items);

            final byte[] unimplemented2 = new byte[0x8]; buffer.get(unimplemented2);
            container.setUnimplemented2(unimplemented2);

            final int firstVertexIndex = buffer.getInt();
            final short numVertices = buffer.getShort();
            final GenericPolygon polygon = new GenericPolygon(
                boundingBoxLeft, boundingBoxRight,
                boundingBoxTop, boundingBoxBottom
            );
            parseVertices(polygon, verticesOffset, firstVertexIndex, numVertices);
            container.setPolygon(polygon);

            final byte[] unimplemented3 = new byte[0x6A]; buffer.get(unimplemented3);
            container.setUnimplemented3(unimplemented3);

            containers.addTail(container);
        }
    }

    private void parseItems(
        final SimpleLinkedList<Item> list,
        final int itemsOffset, final int firstItemIndex, final int numItems)
    {
        final int firstItemOffset = itemsOffset + 0x14 * firstItemIndex;

        mark();
        position(firstItemOffset);
        for (int i = 0; i < numItems; ++i)
        {
            final Item item = new Item();

            final byte[] unimplemented = new byte[0x14]; buffer.get(unimplemented);
            item.setUnimplemented(unimplemented);

            list.addTail(item);
        }
        reset();
    }

    private void parseAmbients(final int ambientsOffset, final short numAmbients)
    {
        position(ambientsOffset);

        for (int i = 0; i < numAmbients; ++i)
        {
            final Ambient ambient = new Ambient();

            final byte[] unimplemented = new byte[0xD4]; buffer.get(unimplemented);
            ambient.setUnimplemented(unimplemented);

            ambients.addTail(ambient);
        }
    }

    private void parseVariables(final int variablesOffset, final short numVariables)
    {
        position(variablesOffset);

        for (int i = 0; i < numVariables; ++i)
        {
            final Variable variable = new Variable();

            final byte[] unimplemented = new byte[0x54]; buffer.get(unimplemented);
            variable.setUnimplemented(unimplemented);

            variables.addTail(variable);
        }
    }

    private void parseExploredBitmask(final int exploredBitmaskOffset, final int exploredBitmaskSize)
    {
        position(exploredBitmaskOffset);

        ExploredBitmask exploredBitmask = new ExploredBitmask();

        final byte[] unimplemented = new byte[exploredBitmaskSize]; buffer.get(unimplemented);
        exploredBitmask.setUnimplemented(unimplemented);

        this.exploredBitmask = exploredBitmask;
    }

    private void parseDoors(final int verticesOffset, final int doorsOffset, final int numDoors)
    {
        position(doorsOffset);

        for (int i = 0; i < numDoors; ++i)
        {
            final Door door = new Door();

            final byte[] unimplemented1 = new byte[0x2C]; buffer.get(unimplemented1);
            door.setUnimplemented1(unimplemented1);

            final int firstOpenOutlineVertexIndex = buffer.getInt();
            final short numOpenOutlineVertices = buffer.getShort();
            final short numClosedOutlineVertices = buffer.getShort();
            final int firstClosedOutlineVertexIndex = buffer.getInt();
            final short openBoundingBoxLeft = buffer.getShort();
            final short openBoundingBoxTop = buffer.getShort();
            final short openBoundingBoxRight = buffer.getShort();
            final short openBoundingBoxBottom = buffer.getShort();
            final short closedBoundingBoxLeft = buffer.getShort();
            final short closedBoundingBoxTop = buffer.getShort();
            final short closedBoundingBoxRight = buffer.getShort();
            final short closedBoundingBoxBottom = buffer.getShort();

            final GenericPolygon openOutlinePolygon = new GenericPolygon(
                openBoundingBoxLeft, openBoundingBoxRight,
                openBoundingBoxTop, openBoundingBoxBottom
            );
            parseVertices(openOutlinePolygon, verticesOffset, firstOpenOutlineVertexIndex, numOpenOutlineVertices);
            door.setOpenOutlinePolygon(openOutlinePolygon);

            final GenericPolygon closedOutlinePolygon = new GenericPolygon(
                closedBoundingBoxLeft, closedBoundingBoxRight,
                closedBoundingBoxTop, closedBoundingBoxBottom
            );
            parseVertices(closedOutlinePolygon, verticesOffset, firstClosedOutlineVertexIndex, numClosedOutlineVertices);
            door.setClosedOutlinePolygon(closedOutlinePolygon);

            final int firstOpenImpededSearchMapCellsVertexIndex = buffer.getInt();
            final short numOpenImpededSearchMapCellsVertices = buffer.getShort();
            final short numClosedImpededSearchMapCellsVertices = buffer.getShort();
            final int firstClosedImpededSearchMapCellsVertexIndex = buffer.getInt();

            final GenericPolygon openImpededSearchMapCellsPolygon = new GenericPolygon(
                0, 0, 0, 0
            );
            parseVertices(openImpededSearchMapCellsPolygon, verticesOffset,
                firstOpenImpededSearchMapCellsVertexIndex, numOpenImpededSearchMapCellsVertices
            );
            door.setOpenImpededSearchMapCellsPolygon(openImpededSearchMapCellsPolygon);

            final GenericPolygon closedImpededSearchMapCellsPolygon = new GenericPolygon(
                0, 0, 0, 0
            );
            parseVertices(closedImpededSearchMapCellsPolygon, verticesOffset,
                firstClosedImpededSearchMapCellsVertexIndex, numClosedImpededSearchMapCellsVertices
            );
            door.setClosedImpededSearchMapCellsPolygon(closedImpededSearchMapCellsPolygon);

            final byte[] unimplemented2 = new byte[0x74]; buffer.get(unimplemented2);
            door.setUnimplemented2(unimplemented2);

            doors.addTail(door);
        }
    }

    private void parseAnimations(final int animationsOffset, final int numAnimations)
    {
        position(animationsOffset);

        for (int i = 0; i < numAnimations; ++i)
        {
            final Animation animation = new Animation();

            final byte[] unimplemented = new byte[0x4C]; buffer.get(unimplemented);
            animation.setUnimplemented(unimplemented);

            animations.addTail(animation);
        }
    }

    private void parseTiledObjects(final int tiledObjectsOffset, final int numTiledObjects)
    {
        if (numTiledObjects == 0)
        {
            return;
        }
        throw new UnsupportedOperationException("Cannot handle tiled objects at this time");

//        position(tiledObjectsOffset);
//
//        for (int i = 0; i < numTiledObjects; ++i)
//        {
//            final TiledObject tiledObject = new TiledObject();
//
//            final byte[] unimplemented = new byte[0x6C]; buffer.get(unimplemented);
//            tiledObject.setUnimplemented(unimplemented);
//
//            tiledObjects.addTail(tiledObject);
//        }
    }

    private void parseSongEntries(final int songEntriesOffset)
    {
        position(songEntriesOffset);

        final SongEntries songEntries = new SongEntries();

        final byte[] unimplemented = new byte[0x90]; buffer.get(unimplemented);
        songEntries.setUnimplemented(unimplemented);

        this.songEntries = songEntries;
    }

    private void parseRestInterruptions(final int restInterruptionsOffset)
    {
        position(restInterruptionsOffset);

        final RestInterruptions restInterruptions = new RestInterruptions();

        final byte[] unimplemented = new byte[0xE4]; buffer.get(unimplemented);
        restInterruptions.setUnimplemented(unimplemented);

        this.restInterruptions = restInterruptions;
    }

    private void parseMapNotes(final int mapNotesOffset, final int numMapNotes)
    {
        position(mapNotesOffset);

        for (int i = 0; i < numMapNotes; ++i)
        {
            final MapNote mapNote = new MapNote();

            final byte[] unimplemented = new byte[0x34]; buffer.get(unimplemented);
            mapNote.setUnimplemented(unimplemented);

            mapNotes.addTail(mapNote);
        }
    }

    private void parseProjectileTraps(final int projectileTrapsOffset, final int numProjectileTraps)
    {
        throw new UnsupportedOperationException("Cannot handle projectile traps at this time");

//        position(projectileTrapsOffset);
//
//        for (int i = 0; i < numProjectileTraps; ++i)
//        {
//            final ProjectileTrap projectileTrap = new ProjectileTrap();
//
//            final byte[] unimplemented = new byte[0x1C]; buffer.get(unimplemented);
//            projectileTrap.setUnimplemented(unimplemented);
//
//            projectileTraps.addTail(projectileTrap);
//        }
    }

    private void parseVertices(
        final GenericPolygon polygon,
        final int verticesOffset, final int firstVertexIndex, final short numVertices)
    {
        final int firstVertexOffset = verticesOffset + 4 * firstVertexIndex;

        mark();
        position(firstVertexOffset);
        for (int i = 0; i < numVertices; ++i)
        {
            final short x = buffer.getShort();
            final short y = buffer.getShort();
            polygon.addVertex(x, y);
        }
        reset();
    }

    //------------//
    // Parse V9.1 //
    //------------//

    private void parse_V9_1(final TaskTrackerI tracker) throws Exception
    {
        throw new UnsupportedOperationException("Cannot handle AREAV9.1 at this time (IWD2 areas).");
        //loadWED(tracker);
    }

    private void loadWED(final TaskTrackerI tracker) throws Exception
    {
        position(0x8);
        final String wedResref = BufferUtil.readLUTF8(buffer, 8);
        final ResourceIdentifier wedIdentifier = new ResourceIdentifier(wedResref, KeyFile.NumericResourceType.WED);
        final Game.Resource wedResource = GlobalState.getGame().getResource(wedIdentifier);

        if (wedResource == null)
        {
            throw new IllegalStateException("Unable to find source for WED resource \"" + wedResref + "\"");
        }

        final WED tempWed = new WED(wedResource.getPrimarySource());
        tempWed.load(tracker);
        wed = tempWed;
    }

    //-----------------//
    // END Loading ARE //
    //-----------------//

    private void mark()
    {
        bufferMarks.push(buffer.position());
    }

    private void reset()
    {
        buffer.position(bufferMarks.pop());
    }

    ////////////////////
    // Public Classes //
    ////////////////////

    public class Graphics
    {
        private final BufferedImage image;
        private final Graphics2D graphics;
        private final WED.Graphics wedGraphics;
        private double zoomFactor = 1;

        public Graphics(final ImageAndGraphics imageAndGraphics)
        {
            this.image = imageAndGraphics.image();
            this.graphics = imageAndGraphics.graphics();
            wedGraphics = wed.newGraphics(imageAndGraphics);
        }

        public Graphics drawActors()
        {
//            graphics.setStroke(new BasicStroke((int)(2 * zoomFactor)));
//            graphics.setFont(new Font("Times New Roman", Font.PLAIN, (int)(18 * zoomFactor)));
//
//            for (final Actor actor : actors)
//            {
//                graphics.drawString(actor.getName(), actor.getX(), actor.getY());
//                graphics.drawOval(actor.getX(), actor.getY(), 100, 100);
//            }

            return this;
        }

        public WED.Graphics getWedGraphics()
        {
            return wedGraphics;
        }

        public BufferedImage getImage()
        {
            return image;
        }

        public void setZoomFactor(final double zoomFactor)
        {
            this.zoomFactor = zoomFactor;
        }

        public void drawImage(final BufferedImage image, final int x, final int y)
        {
            graphics.drawImage(image, x, y, null);
        }
    }

    public class Actor implements ReferenceTrackable
    {
        ////////////////////
        // Private Fields //
        ////////////////////

        private final ReferenceTracker referenceTracker = new ReferenceTracker(this);

        private String name;
        private short x;
        private short y;
        private byte[] unimplemented1;
        private short orientation;
        private byte[] unimplemented2;
        private String creatureResref;
        private Creature creature;
        private byte[] unimplemented3;

        ///////////////////////////
        // Public Static Methods //
        ///////////////////////////

        public static double orientationToDegree(final short orientation)
        {
            return switch (orientation)
            {
                case 0 -> 270;  // S
                case 1 -> 255;  // SSW
                case 2 -> 225;  // SW
                case 3 -> 195;  // SWW
                case 4 -> 180;  // W
                case 5 -> 165;  // NWW
                case 6 -> 135;  // NW
                case 7 -> 105;  // NNW
                case 8 -> 90;   // N
                case 9 -> 75;   // NNE
                case 10 -> 45;  // NE
                case 11 -> 15;  // NEE
                case 12 -> 0;   // E
                case 13 -> 345; // SEE
                case 14 -> 315; // SE
                case 15 -> 285; // SSE
                default -> throw new IllegalStateException();
            };
        }

        ////////////////////
        // Public Methods //
        ////////////////////

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public short getX()
        {
            return x;
        }

        public void setX(short x)
        {
            this.x = x;
        }

        public short getY()
        {
            return y;
        }

        public void setY(short y)
        {
            this.y = y;
        }

        public byte[] getUnimplemented1()
        {
            return unimplemented1;
        }

        public void setUnimplemented1(byte[] unimplemented1)
        {
            this.unimplemented1 = unimplemented1;
        }

        public short getOrientation()
        {
            return orientation;
        }

        public void setOrientation(short orientation)
        {
            this.orientation = orientation;
        }

        public byte[] getUnimplemented2()
        {
            return unimplemented2;
        }

        public void setUnimplemented2(byte[] unimplemented2)
        {
            this.unimplemented2 = unimplemented2;
        }

        public String getCreatureResref()
        {
            return creatureResref;
        }

        public void setCreatureResref(String creatureResref)
        {
            this.creatureResref = creatureResref;
        }

        public Creature getCreature()
        {
            return creature;
        }

        public void setCreature(Creature creature)
        {
            this.creature = creature;
        }

        public byte[] getUnimplemented3()
        {
            return unimplemented3;
        }

        public void setUnimplemented3(byte[] unimplemented3)
        {
            this.unimplemented3 = unimplemented3;
        }

        public double getOrientationDegree()
        {
            return orientationToDegree(orientation);
        }

        @Override
        public void addedTo(final ReferenceHolder<?> referenceHolder)
        {
            referenceTracker.addedTo(referenceHolder);
        }

        @Override
        public void removedFrom(final ReferenceHolder<?> referenceHolder)
        {
            referenceTracker.removedFrom(referenceHolder);
        }

        @Override
        public void delete()
        {
            referenceTracker.delete();
        }
    }

    // Size: 0xC4
    public static class Region implements ReferenceTrackable
    {
        ////////////////////
        // Private Fields //
        ////////////////////

        // Extra
        private final ReferenceTracker referenceTracker = new ReferenceTracker(this);

        // Mirrors in-file data
        private String name;
        private short type;
        private GenericPolygon polygon;
        private int triggerValue;
        private int cursorIndex; // CURSORS.BAM
        private String destAreaResref; // For type=2
        private String entranceNameInDestArea; // For type=2
        private int flags;
        private int infoStrref; // For type=1
        private short trapDetectionDifficulty;
        private short trapDisarmDifficulty;
        private short bTrapped; // 0 = No, 1 = Yes
        private short bTrapDetected; // 0 = No, 1 = Yes
        private IntPoint trapLaunchPoint;
        private String keyResref; // For type=?
        private String scriptResref; // For type=?
        private short activationPointX;
        private short activationPointY;
        private byte[] unknown;

        /////////////////////////
        // Public Constructors //
        /////////////////////////

        public Region()
        {
            name = "";
            polygon = new GenericPolygon(0, 0, 0, 0);
            destAreaResref = "";
            entranceNameInDestArea = "";
            trapLaunchPoint = new IntPointImpl(0, 0);
            keyResref = "";
            scriptResref = "";
            unknown = new byte[0x3C];
        }

        //////////////////////////
        // Private Constructors //
        //////////////////////////

        private Region(final NO_INIT ignored) {}

        ////////////////////
        // Public Methods //
        ////////////////////

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public short getType()
        {
            return type;
        }

        public void setType(final short type)
        {
            this.type = type;
        }

        public GenericPolygon getPolygon()
        {
            return polygon;
        }

        public void setPolygon(GenericPolygon polygon)
        {
            this.polygon = polygon;
        }

        public int getTriggerValue()
        {
            return triggerValue;
        }

        public void setTriggerValue(final int triggerValue)
        {
            this.triggerValue = triggerValue;
        }

        public int getCursorIndex()
        {
            return cursorIndex;
        }

        public void setCursorIndex(final int cursorIndex)
        {
            this.cursorIndex = cursorIndex;
        }

        public String getDestAreaResref()
        {
            return destAreaResref;
        }

        public void setDestAreaResref(final String destAreaResref)
        {
            this.destAreaResref = destAreaResref;
        }

        public String getEntranceNameInDestArea()
        {
            return entranceNameInDestArea;
        }

        public void setEntranceNameInDestArea(final String entranceNameInDestArea)
        {
            this.entranceNameInDestArea = entranceNameInDestArea;
        }

        public int getFlags()
        {
            return flags;
        }

        public void setFlags(final int flags)
        {
            this.flags = flags;
        }

        public int getInfoStrref()
        {
            return infoStrref;
        }

        public void setInfoStrref(final int infoStrref)
        {
            this.infoStrref = infoStrref;
        }

        public short getTrapDetectionDifficulty()
        {
            return trapDetectionDifficulty;
        }

        public void setTrapDetectionDifficulty(final short trapDetectionDifficulty)
        {
            this.trapDetectionDifficulty = trapDetectionDifficulty;
        }

        public short getTrapDisarmDifficulty()
        {
            return trapDisarmDifficulty;
        }

        public void setTrapDisarmDifficulty(final short trapDisarmDifficulty)
        {
            this.trapDisarmDifficulty = trapDisarmDifficulty;
        }

        public short getbTrapped()
        {
            return bTrapped;
        }

        public void setbTrapped(final short bTrapped)
        {
            this.bTrapped = bTrapped;
        }

        public short getbTrapDetected()
        {
            return bTrapDetected;
        }

        public void setbTrapDetected(final short bTrapDetected)
        {
            this.bTrapDetected = bTrapDetected;
        }

        public IntPoint getTrapLaunchPoint()
        {
            return trapLaunchPoint;
        }

        public void setTrapLaunchPoint(final IntPoint trapLaunchPoint)
        {
            this.trapLaunchPoint = trapLaunchPoint;
        }

        public String getKeyResref()
        {
            return keyResref;
        }

        public void setKeyResref(final String keyResref)
        {
            this.keyResref = keyResref;
        }

        public String getScriptResref()
        {
            return scriptResref;
        }

        public void setScriptResref(final String scriptResref)
        {
            this.scriptResref = scriptResref;
        }

        public short getActivationPointX()
        {
            return activationPointX;
        }

        public void setActivationPointX(final short activationPointX)
        {
            this.activationPointX = activationPointX;
        }

        public short getActivationPointY()
        {
            return activationPointY;
        }

        public void setActivationPointY(final short activationPointY)
        {
            this.activationPointY = activationPointY;
        }

        public byte[] getUnknown()
        {
            return unknown;
        }

        public void setUnknown(final byte[] unknown)
        {
            this.unknown = unknown;
        }

        @Override
        public void addedTo(final ReferenceHolder<?> referenceHolder)
        {
            referenceTracker.addedTo(referenceHolder);
        }

        @Override
        public void removedFrom(final ReferenceHolder<?> referenceHolder)
        {
            referenceTracker.removedFrom(referenceHolder);
        }

        @Override
        public void delete()
        {
            referenceTracker.delete();
        }
    }

    // Size: 0xC8
    public class SpawnPoint implements ReferenceTrackable
    {
        ////////////////////
        // Private Fields //
        ////////////////////

        private final ReferenceTracker referenceTracker = new ReferenceTracker(this);

        private byte[] unimplemented;

        ////////////////////
        // Public Methods //
        ////////////////////

        public byte[] getUnimplemented()
        {
            return unimplemented;
        }

        public void setUnimplemented(byte[] unimplemented)
        {
            this.unimplemented = unimplemented;
        }

        @Override
        public void addedTo(final ReferenceHolder<?> referenceHolder)
        {
            referenceTracker.addedTo(referenceHolder);
        }

        @Override
        public void removedFrom(final ReferenceHolder<?> referenceHolder)
        {
            referenceTracker.removedFrom(referenceHolder);
        }

        @Override
        public void delete()
        {
            referenceTracker.delete();
        }
    }

    // Size: 0x68
    public class Entrance implements ReferenceTrackable
    {
        ////////////////////
        // Private Fields //
        ////////////////////

        private final ReferenceTracker referenceTracker = new ReferenceTracker(this);

        private byte[] unimplemented;

        ////////////////////
        // Public Methods //
        ////////////////////

        public byte[] getUnimplemented()
        {
            return unimplemented;
        }

        public void setUnimplemented(byte[] unimplemented)
        {
            this.unimplemented = unimplemented;
        }

        @Override
        public void addedTo(final ReferenceHolder<?> referenceHolder)
        {
            referenceTracker.addedTo(referenceHolder);
        }

        @Override
        public void removedFrom(final ReferenceHolder<?> referenceHolder)
        {
            referenceTracker.removedFrom(referenceHolder);
        }

        @Override
        public void delete()
        {
            referenceTracker.delete();
        }
    }

    // Size: 0xC0
    public class Container implements ReferenceTrackable
    {
        ////////////////////
        // Private Fields //
        ////////////////////

        private final ReferenceTracker referenceTracker = new ReferenceTracker(this);

        private byte[] unimplemented1;
        private TrackingOrderedInstanceSet<Item> items;
        private byte[] unimplemented2;
        private GenericPolygon polygon;
        private byte[] unimplemented3;

        ////////////////////
        // Public Methods //
        ////////////////////

        public byte[] getUnimplemented1()
        {
            return unimplemented1;
        }

        public void setUnimplemented1(byte[] unimplemented1)
        {
            this.unimplemented1 = unimplemented1;
        }

        public OrderedInstanceSet<Item> getItems()
        {
            return items;
        }

        public void setItems(TrackingOrderedInstanceSet<Item> items)
        {
            this.items = items;
        }

        public byte[] getUnimplemented2()
        {
            return unimplemented2;
        }

        public void setUnimplemented2(byte[] unimplemented2)
        {
            this.unimplemented2 = unimplemented2;
        }

        public GenericPolygon getPolygon()
        {
            return polygon;
        }

        public void setPolygon(GenericPolygon polygon)
        {
            this.polygon = polygon;
        }

        public byte[] getUnimplemented3()
        {
            return unimplemented3;
        }

        public void setUnimplemented3(byte[] unimplemented3)
        {
            this.unimplemented3 = unimplemented3;
        }

        @Override
        public void addedTo(final ReferenceHolder<?> referenceHolder)
        {
            referenceTracker.addedTo(referenceHolder);
        }

        @Override
        public void removedFrom(final ReferenceHolder<?> referenceHolder)
        {
            referenceTracker.removedFrom(referenceHolder);
        }

        @Override
        public void delete()
        {
            referenceTracker.delete();
        }
    }

    // Size: 0x14
    public class Item implements ReferenceTrackable
    {
        ////////////////////
        // Private Fields //
        ////////////////////

        private final ReferenceTracker referenceTracker = new ReferenceTracker(this);

        private byte[] unimplemented;

        ////////////////////
        // Public Methods //
        ////////////////////

        public byte[] getUnimplemented()
        {
            return unimplemented;
        }

        public void setUnimplemented(byte[] unimplemented)
        {
            this.unimplemented = unimplemented;
        }

        @Override
        public void addedTo(final ReferenceHolder<?> referenceHolder)
        {
            referenceTracker.addedTo(referenceHolder);
        }

        @Override
        public void removedFrom(final ReferenceHolder<?> referenceHolder)
        {
            referenceTracker.removedFrom(referenceHolder);
        }

        @Override
        public void delete()
        {
            referenceTracker.delete();
        }
    }

    // Size: 0xD4
    public class Ambient implements ReferenceTrackable
    {
        ////////////////////
        // Private Fields //
        ////////////////////

        private final ReferenceTracker referenceTracker = new ReferenceTracker(this);

        private byte[] unimplemented;

        ////////////////////
        // Public Methods //
        ////////////////////

        public byte[] getUnimplemented()
        {
            return unimplemented;
        }

        public void setUnimplemented(byte[] unimplemented)
        {
            this.unimplemented = unimplemented;
        }

        @Override
        public void addedTo(final ReferenceHolder<?> referenceHolder)
        {
            referenceTracker.addedTo(referenceHolder);
        }

        @Override
        public void removedFrom(final ReferenceHolder<?> referenceHolder)
        {
            referenceTracker.removedFrom(referenceHolder);
        }

        @Override
        public void delete()
        {
            referenceTracker.delete();
        }
    }

    // Size: 0x54
    public class Variable implements ReferenceTrackable
    {
        ////////////////////
        // Private Fields //
        ////////////////////

        private final ReferenceTracker referenceTracker = new ReferenceTracker(this);

        private byte[] unimplemented;

        ////////////////////
        // Public Methods //
        ////////////////////

        public byte[] getUnimplemented()
        {
            return unimplemented;
        }

        public void setUnimplemented(byte[] unimplemented)
        {
            this.unimplemented = unimplemented;
        }

        @Override
        public void addedTo(final ReferenceHolder<?> referenceHolder)
        {
            referenceTracker.addedTo(referenceHolder);
        }

        @Override
        public void removedFrom(final ReferenceHolder<?> referenceHolder)
        {
            referenceTracker.removedFrom(referenceHolder);
        }

        @Override
        public void delete()
        {
            referenceTracker.delete();
        }
    }

    // Size: Variable (depends on map size)
    public class ExploredBitmask
    {
        ////////////////////
        // Private Fields //
        ////////////////////

        private byte[] unimplemented;

        ////////////////////
        // Public Methods //
        ////////////////////

        public byte[] getUnimplemented()
        {
            return unimplemented;
        }

        public void setUnimplemented(byte[] unimplemented)
        {
            this.unimplemented = unimplemented;
        }
    }

    // Size: 0xC8
    public class Door implements ReferenceTrackable
    {
        ////////////////////
        // Private Fields //
        ////////////////////

        private final ReferenceTracker referenceTracker = new ReferenceTracker(this);

        private byte[] unimplemented1;
        private GenericPolygon openOutlinePolygon;
        private GenericPolygon closedOutlinePolygon;
        private GenericPolygon openImpededSearchMapCellsPolygon;
        private GenericPolygon closedImpededSearchMapCellsPolygon;
        private byte[] unimplemented2;

        ////////////////////
        // Public Methods //
        ////////////////////

        public byte[] getUnimplemented1()
        {
            return unimplemented1;
        }

        public void setUnimplemented1(byte[] unimplemented1)
        {
            this.unimplemented1 = unimplemented1;
        }

        public GenericPolygon getOpenOutlinePolygon()
        {
            return openOutlinePolygon;
        }

        public void setOpenOutlinePolygon(GenericPolygon openOutlinePolygon)
        {
            this.openOutlinePolygon = openOutlinePolygon;
        }

        public GenericPolygon getClosedOutlinePolygon()
        {
            return closedOutlinePolygon;
        }

        public void setClosedOutlinePolygon(GenericPolygon closedOutlinePolygon)
        {
            this.closedOutlinePolygon = closedOutlinePolygon;
        }

        public GenericPolygon getOpenImpededSearchMapCellsPolygon()
        {
            return openImpededSearchMapCellsPolygon;
        }

        public void setOpenImpededSearchMapCellsPolygon(GenericPolygon openImpededSearchMapCellsPolygon)
        {
            this.openImpededSearchMapCellsPolygon = openImpededSearchMapCellsPolygon;
        }

        public GenericPolygon getClosedImpededSearchMapCellsPolygon()
        {
            return closedImpededSearchMapCellsPolygon;
        }

        public void setClosedImpededSearchMapCellsPolygon(GenericPolygon closedImpededSearchMapCellsPolygon)
        {
            this.closedImpededSearchMapCellsPolygon = closedImpededSearchMapCellsPolygon;
        }

        public byte[] getUnimplemented2()
        {
            return unimplemented2;
        }

        public void setUnimplemented2(byte[] unimplemented2)
        {
            this.unimplemented2 = unimplemented2;
        }

        @Override
        public void addedTo(final ReferenceHolder<?> referenceHolder)
        {
            referenceTracker.addedTo(referenceHolder);
        }

        @Override
        public void removedFrom(final ReferenceHolder<?> referenceHolder)
        {
            referenceTracker.removedFrom(referenceHolder);
        }

        @Override
        public void delete()
        {
            referenceTracker.delete();
        }
    }

    // Size: 0x4C
    public class Animation implements ReferenceTrackable
    {
        ////////////////////
        // Private Fields //
        ////////////////////

        private final ReferenceTracker referenceTracker = new ReferenceTracker(this);

        private byte[] unimplemented;

        ////////////////////
        // Public Methods //
        ////////////////////

        public byte[] getUnimplemented()
        {
            return unimplemented;
        }

        public void setUnimplemented(byte[] unimplemented)
        {
            this.unimplemented = unimplemented;
        }

        @Override
        public void addedTo(final ReferenceHolder<?> referenceHolder)
        {
            referenceTracker.addedTo(referenceHolder);
        }

        @Override
        public void removedFrom(final ReferenceHolder<?> referenceHolder)
        {
            referenceTracker.removedFrom(referenceHolder);
        }

        @Override
        public void delete()
        {
            referenceTracker.delete();
        }
    }

    // Size: 0x34
    public class MapNote implements ReferenceTrackable
    {
        ////////////////////
        // Private Fields //
        ////////////////////

        private final ReferenceTracker referenceTracker = new ReferenceTracker(this);

        private byte[] unimplemented;

        ////////////////////
        // Public Methods //
        ////////////////////

        public byte[] getUnimplemented()
        {
            return unimplemented;
        }

        public void setUnimplemented(byte[] unimplemented)
        {
            this.unimplemented = unimplemented;
        }

        @Override
        public void addedTo(final ReferenceHolder<?> referenceHolder)
        {
            referenceTracker.addedTo(referenceHolder);
        }

        @Override
        public void removedFrom(final ReferenceHolder<?> referenceHolder)
        {
            referenceTracker.removedFrom(referenceHolder);
        }

        @Override
        public void delete()
        {
            referenceTracker.delete();
        }
    }

    // Size: 0x6C
    public class TiledObject implements ReferenceTrackable
    {
        ////////////////////
        // Private Fields //
        ////////////////////

        private final ReferenceTracker referenceTracker = new ReferenceTracker(this);

        private byte[] unimplemented;
        // TODO: Has offsets (search map squares)

        ////////////////////
        // Public Methods //
        ////////////////////

        public byte[] getUnimplemented()
        {
            return unimplemented;
        }

        public void setUnimplemented(byte[] unimplemented)
        {
            this.unimplemented = unimplemented;
        }

        @Override
        public void addedTo(final ReferenceHolder<?> referenceHolder)
        {
            referenceTracker.addedTo(referenceHolder);
        }

        @Override
        public void removedFrom(final ReferenceHolder<?> referenceHolder)
        {
            referenceTracker.removedFrom(referenceHolder);
        }

        @Override
        public void delete()
        {
            referenceTracker.delete();
        }
    }

    // Size: 0x1C
    public class ProjectileTrap implements ReferenceTrackable
    {
        ////////////////////
        // Private Fields //
        ////////////////////

        private final ReferenceTracker referenceTracker = new ReferenceTracker(this);

        private byte[] unimplemented;
        // TODO: Has offsets (effects)

        ////////////////////
        // Public Methods //
        ////////////////////

        public byte[] getUnimplemented()
        {
            return unimplemented;
        }

        public void setUnimplemented(byte[] unimplemented)
        {
            this.unimplemented = unimplemented;
        }

        @Override
        public void addedTo(final ReferenceHolder<?> referenceHolder)
        {
            referenceTracker.addedTo(referenceHolder);
        }

        @Override
        public void removedFrom(final ReferenceHolder<?> referenceHolder)
        {
            referenceTracker.removedFrom(referenceHolder);
        }

        @Override
        public void delete()
        {
            referenceTracker.delete();
        }
    }

    // Size: 0x90
    public class SongEntries
    {
        ////////////////////
        // Private Fields //
        ////////////////////

        private byte[] unimplemented;

        ////////////////////
        // Public Methods //
        ////////////////////

        public byte[] getUnimplemented()
        {
            return unimplemented;
        }

        public void setUnimplemented(byte[] unimplemented)
        {
            this.unimplemented = unimplemented;
        }
    }

    // Size: 0xE4
    public class RestInterruptions
    {
        ////////////////////
        // Private Fields //
        ////////////////////

        private byte[] unimplemented;

        ////////////////////
        // Public Methods //
        ////////////////////

        public byte[] getUnimplemented()
        {
            return unimplemented;
        }

        public void setUnimplemented(byte[] unimplemented)
        {
            this.unimplemented = unimplemented;
        }
    }

    public class Creature
    {

    }

    /////////////////////
    // Private Classes //
    /////////////////////

    private class SaveAreaState
    {
        ////////////////////
        // Private Fields //
        ////////////////////

        private final Path path;
        private ByteBuffer buffer;

        private int doorVerticesFirstIndex;
        private int regionVerticesFirstIndex;
        private short numTotalItems;
        private short numTotalVertices;

        /////////////////////////
        // Public Constructors //
        /////////////////////////

        public SaveAreaState(final Path path)
        {
            this.path = path;
        }

        ////////////////////
        // Public Methods //
        ////////////////////

        public void save(final TaskTrackerI tracker) throws Exception
        {
            final AreaSectionSizes sectionSizes = calculateSectionSizes();
            buffer = ByteBuffer.allocate(sectionSizes.total());
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            final int headerOffset = 0x0;
            final int songEntriesOffset = headerOffset + sectionSizes.headerSize();
            final int restInterruptionsOffset = songEntriesOffset + sectionSizes.songEntriesSize();
            final int exploredBitmaskOffset = restInterruptionsOffset + sectionSizes.restInterruptionsSize();
            final int projectileTrapEffectsArrayOffset = exploredBitmaskOffset + sectionSizes.exploredBitmaskSize();
            final int projectileTrapsArrayOffset = projectileTrapEffectsArrayOffset + sectionSizes.projectileTrapEffectsArraySize();
            final int animationsArrayOffset = projectileTrapsArrayOffset + sectionSizes.projectileTrapsArraySize();
            final int tiledObjectsArrayOffset = animationsArrayOffset; // TODO: Not saved
            final int ambientsArrayOffset = animationsArrayOffset + sectionSizes.animationsArraySize();
            final int spawnPointsArrayOffset = ambientsArrayOffset + sectionSizes.ambientsArraySize();
            final int creatureArrayOffset = spawnPointsArrayOffset + sectionSizes.spawnPointsArraySize();
            final int actorsArrayOffset = creatureArrayOffset + sectionSizes.creatureArraySize();
            final int verticesArrayOffset = actorsArrayOffset + sectionSizes.actorsArraySize();
            final int doorsArrayOffset = verticesArrayOffset + sectionSizes.verticesArraySize();
            final int regionsArrayOffset = doorsArrayOffset + sectionSizes.doorsArraySize();
            final int itemsArrayOffset = regionsArrayOffset + sectionSizes.regionsArraySize();
            final int containersArrayOffset = itemsArrayOffset + sectionSizes.itemsArraySize();
            final int variablesArrayOffset = containersArrayOffset + sectionSizes.containersArraySize();
            final int entrancesArrayOffset = variablesArrayOffset + sectionSizes.variablesArraySize();
            final int mapNotesArrayOffset = entrancesArrayOffset + sectionSizes.entrancesArraySize();

            saveHeader(
                headerOffset,
                actorsArrayOffset,
                regionsArrayOffset,
                spawnPointsArrayOffset,
                entrancesArrayOffset,
                containersArrayOffset,
                itemsArrayOffset,
                verticesArrayOffset,
                ambientsArrayOffset,
                variablesArrayOffset,
                exploredBitmaskOffset,
                doorsArrayOffset,
                animationsArrayOffset,
                tiledObjectsArrayOffset,
                songEntriesOffset,
                restInterruptionsOffset,
                mapNotesArrayOffset,
                projectileTrapsArrayOffset
            );

            saveSongEntries(songEntriesOffset);
            saveRestInterruptions(restInterruptionsOffset);
            saveExploredBitmask(exploredBitmaskOffset);

            if (hasProjectileTraps())
            {
                saveProjectileTrapEffectsArray(projectileTrapEffectsArrayOffset);
                saveProjectileTrapsArray(projectileTrapsArrayOffset);
            }

            saveAnimationsArray(animationsArrayOffset);
            saveAmbientsArray(ambientsArrayOffset);
            saveSpawnPointsArray(spawnPointsArrayOffset);
            saveCreatureArray(creatureArrayOffset);
            saveActorsArray(actorsArrayOffset);
            saveVerticesArray(verticesArrayOffset);
            saveDoorsArray(doorsArrayOffset);
            saveRegionsArray(regionsArrayOffset);
            saveItemsArray(itemsArrayOffset);
            saveContainersArray(containersArrayOffset);
            saveVariablesArray(variablesArrayOffset);
            saveEntrancesArray(entrancesArrayOffset);

            if (hasMapNotes())
            {
                saveMapNotesArray(mapNotesArrayOffset);
            }

            Files.write(path, buffer.array());
        }

        /////////////////////
        // Private Methods //
        /////////////////////

        /////////////////////
        // Saving Sections //
        /////////////////////

        private void saveHeader(
            final int offset,
            final int actorsArrayOffset,
            final int regionsArrayOffset,
            final int spawnPointsArrayOffset,
            final int entrancesArrayOffset,
            final int containersArrayOffset,
            final int itemsArrayOffset,
            final int verticesArrayOffset,
            final int ambientsArrayOffset,
            final int variablesArrayOffset,
            final int exploredBitmaskOffset,
            final int doorsArrayOffset,
            final int animationsArrayOffset,
            final int tiledObjectsArrayOffset,
            final int songEntriesOffset,
            final int restInterruptionsOffset,
            final int mapNotesArrayOffset,
            final int projectileTrapsArrayOffset
        )
        {
            buffer.position(offset);
            buffer.put("AREA".getBytes(StandardCharsets.UTF_8));
            buffer.put("V1.0".getBytes(StandardCharsets.UTF_8));
            BufferUtil.writeLUTF8(buffer, 8, wed.getSource().getIdentifier().resref());

            buffer.put(unimplemented1);

            buffer.putInt(actorsArrayOffset);
            buffer.putShort((short)actors.size());

            buffer.putShort((short)regions.size());
            buffer.putInt(regionsArrayOffset);

            buffer.putInt(spawnPointsArrayOffset);
            buffer.putInt(spawnPoints.size());

            buffer.putInt(entrancesArrayOffset);
            buffer.putInt(entrances.size());

            buffer.putInt(containersArrayOffset);
            buffer.putShort((short)containers.size());

            buffer.putShort(numTotalItems);
            buffer.putInt(itemsArrayOffset);

            buffer.putInt(verticesArrayOffset);
            buffer.putShort(numTotalVertices);

            buffer.putShort((short)ambients.size());
            buffer.putInt(ambientsArrayOffset);

            buffer.putInt(variablesArrayOffset);
            buffer.putShort((short)variables.size());

            buffer.putShort((short)0); // Unused numObjectFlags
            buffer.putInt(0); // Unused objectFlagsOffset
            BufferUtil.writeLUTF8(buffer, 8, areaScript);

            buffer.putInt(exploredBitmask.getUnimplemented().length);
            buffer.putInt(exploredBitmaskOffset);

            buffer.putInt(doors.size());
            buffer.putInt(doorsArrayOffset);

            buffer.putInt(animations.size());
            buffer.putInt(animationsArrayOffset);

            buffer.putInt(tiledObjects.size());
            buffer.putInt(tiledObjectsArrayOffset);

            buffer.putInt(songEntriesOffset);
            buffer.putInt(restInterruptionsOffset);

            if (GlobalState.getGame().getEngineType() == Game.Type.PST)
            {
                buffer.putInt(specialPSTField);
            }

            if (hasMapNotes())
            {
                buffer.putInt(mapNotesArrayOffset);
                buffer.putInt(mapNotes.size());
            }

            if (hasProjectileTraps())
            {
                buffer.putInt(projectileTrapsArrayOffset);
                buffer.putInt(projectileTraps.size());
            }

            buffer.put(unimplemented2);
        }

        private void saveSongEntries(final int offset)
        {
            buffer.position(offset);
            buffer.put(songEntries.getUnimplemented());
        }

        private void saveRestInterruptions(final int offset)
        {
            buffer.position(offset);
            buffer.put(restInterruptions.getUnimplemented());
        }

        private void saveExploredBitmask(final int offset)
        {
            buffer.position(offset);
            buffer.put(exploredBitmask.getUnimplemented());
        }

        private void saveProjectileTrapEffectsArray(final int offset)
        {
            buffer.position(offset);
            // TODO: Unimplemented
        }

        private void saveProjectileTrapsArray(final int offset)
        {
            buffer.position(offset);
            // TODO: Unimplemented
        }

        private void saveAnimationsArray(final int offset)
        {
            buffer.position(offset);

            for (final Animation animation : animations)
            {
                buffer.put(animation.getUnimplemented());
            }
        }

        private void saveAmbientsArray(final int offset)
        {
            buffer.position(offset);

            for (final Ambient ambient : ambients)
            {
                buffer.put(ambient.getUnimplemented());
            }
        }

        private void saveSpawnPointsArray(final int offset)
        {
            buffer.position(offset);

            for (final SpawnPoint spawnPoint : spawnPoints)
            {
                buffer.put(spawnPoint.getUnimplemented());
            }
        }

        private void saveCreatureArray(final int offset)
        {
            buffer.position(offset);
            // TODO: Unimplemented
        }

        private void saveActorsArray(final int offset)
        {
            buffer.position(offset);

            for (final Actor actor : actors)
            {
                BufferUtil.writeLUTF8(buffer, 32, actor.getName());
                buffer.putShort(actor.getX());
                buffer.putShort(actor.getY());

                buffer.put(actor.getUnimplemented1());

                buffer.putShort(actor.getOrientation());

                buffer.put(actor.getUnimplemented2());

                BufferUtil.writeLUTF8(buffer, 8, actor.getCreatureResref());
                buffer.putInt(0); // TODO: Unimplemented (creature offset)
                buffer.putInt(0); // TODO: Unimplemented (creature size)

                buffer.put(actor.getUnimplemented3());
            }
        }

        private void saveVerticesArray(final int offset)
        {
            buffer.position(offset);

            for (final Container container : containers)
            {
                writePolygonVertices(container.getPolygon());
            }

            for (final Door door : doors)
            {
                writePolygonVertices(door.getOpenOutlinePolygon());
                writePolygonVertices(door.getClosedOutlinePolygon());
                writePolygonVertices(door.getOpenImpededSearchMapCellsPolygon());
                writePolygonVertices(door.getClosedImpededSearchMapCellsPolygon());
            }

            for (final Region region : regions)
            {
                writePolygonVertices(region.getPolygon());
            }
        }

        private void writePolygonVertices(final GenericPolygon polygon)
        {
            for (final GenericPolygon.Vertex vertex : polygon.getVertices())
            {
                buffer.putShort((short)vertex.x());
                buffer.putShort((short)vertex.y());
            }
        }

        private void saveDoorsArray(final int offset)
        {
            buffer.position(offset);
            int curVertexIndex = doorVerticesFirstIndex;

            for (final Door door : doors)
            {
                buffer.put(door.getUnimplemented1());

                final GenericPolygon openOutlinePolygon = door.getOpenOutlinePolygon();
                final short numOpenOutlineVertices = (short)openOutlinePolygon.getVertices().size();
                buffer.putInt(curVertexIndex);
                buffer.putShort(numOpenOutlineVertices);
                curVertexIndex += numOpenOutlineVertices;

                final GenericPolygon closedOutlinePolygon = door.getClosedOutlinePolygon();
                final short numClosedOutlineVertices = (short)closedOutlinePolygon.getVertices().size();
                buffer.putShort(numClosedOutlineVertices);
                buffer.putInt(curVertexIndex);
                curVertexIndex += numClosedOutlineVertices;

                savePolygonBounds(openOutlinePolygon);
                savePolygonBounds(closedOutlinePolygon);

                final short numOpenImpededVertices = (short)door.getOpenImpededSearchMapCellsPolygon().getVertices().size();
                buffer.putInt(curVertexIndex);
                buffer.putShort(numOpenImpededVertices);
                curVertexIndex += numOpenImpededVertices;

                final short numClosedImpededVertices = (short)door.getClosedImpededSearchMapCellsPolygon().getVertices().size();
                buffer.putShort(numClosedImpededVertices);
                buffer.putInt(curVertexIndex);
                curVertexIndex += numClosedImpededVertices;

                buffer.put(door.getUnimplemented2());
            }
        }

        private void saveRegionsArray(final int offset)
        {
            buffer.position(offset);
            int curVertexIndex = regionVerticesFirstIndex;

            for (final Region region : regions)
            {
                BufferUtil.writeLUTF8(buffer, 32, region.getName());
                buffer.putShort(region.getType());

                final GenericPolygon polygon = region.getPolygon();
                final short numVertices = (short)polygon.getVertices().size();
                savePolygonBounds(polygon);
                buffer.putShort(numVertices);
                buffer.putInt(curVertexIndex);
                curVertexIndex += numVertices;

                buffer.putInt(region.getTriggerValue());
                buffer.putInt(region.getCursorIndex());
                BufferUtil.writeLUTF8(buffer, 8, region.getDestAreaResref());
                BufferUtil.writeLUTF8(buffer, 32, region.getEntranceNameInDestArea());
                buffer.putInt(region.getFlags());
                buffer.putInt(region.getInfoStrref());
                buffer.putShort(region.getTrapDetectionDifficulty());
                buffer.putShort(region.getTrapDisarmDifficulty());
                buffer.putShort(region.getbTrapped());
                buffer.putShort(region.getbTrapDetected());
                final IntPoint trapLaunchPoint = region.getTrapLaunchPoint();
                buffer.putShort((short)trapLaunchPoint.getX());
                buffer.putShort((short)trapLaunchPoint.getY());
                BufferUtil.writeLUTF8(buffer, 8, region.getKeyResref());
                BufferUtil.writeLUTF8(buffer, 8, region.getScriptResref());
                buffer.putShort(region.getActivationPointX());
                buffer.putShort(region.getActivationPointY());
                buffer.put(region.getUnknown());
            }
        }

        private void saveItemsArray(final int offset)
        {
            buffer.position(offset);

            for (final Container container : containers)
            {
                for (final Item item : container.getItems())
                {
                    buffer.put(item.getUnimplemented());
                }
            }
        }

        private void saveContainersArray(final int offset)
        {
            buffer.position(offset);
            int curItemIndex = 0;
            int curVertexIndex = 0;

            for (final Container container : containers)
            {
                buffer.put(container.getUnimplemented1());

                final GenericPolygon polygon = container.getPolygon();
                savePolygonBounds(polygon);

                final int numItems = container.getItems().size();
                buffer.putInt(curItemIndex);
                buffer.putInt(numItems);
                curItemIndex += numItems;

                buffer.put(container.getUnimplemented2());

                final short numVertices = (short)polygon.getVertices().size();
                buffer.putInt(curVertexIndex);
                buffer.putShort(numVertices);
                curVertexIndex += numVertices;

                buffer.put(container.getUnimplemented3());
            }
        }

        private void savePolygonBounds(final GenericPolygon polygon)
        {
            buffer.putShort((short)polygon.getBoundingBoxLeft());
            buffer.putShort((short)polygon.getBoundingBoxTop());
            buffer.putShort((short)polygon.getBoundingBoxRight());
            buffer.putShort((short)polygon.getBoundingBoxBottom());
        }

        private void saveVariablesArray(final int offset)
        {
            buffer.position(offset);

            for (final Variable variable : variables)
            {
                buffer.put(variable.getUnimplemented());
            }
        }

        private void saveEntrancesArray(final int offset)
        {
            buffer.position(offset);

            for (final Entrance entrance : entrances)
            {
                buffer.put(entrance.getUnimplemented());
            }
        }

        private void saveMapNotesArray(final int offset)
        {
            buffer.position(offset);

            for (final MapNote mapNote : mapNotes)
            {
                buffer.put(mapNote.getUnimplemented());
            }
        }

        ///////////////////////////////
        // Calculating Section Sizes //
        ///////////////////////////////

        private AreaSectionSizes calculateSectionSizes()
        {
            final int headerSize = calculateHeaderSize();
            final int songEntriesSize = calculateSongEntriesSize();
            final int restInterruptionsSize = calculateRestInterruptionsSize();
            final int exploredBitmaskSize = calculateExploredBitmaskSize();
            final int projectileTrapEffectsArraySize = calculateProjectileTrapEffectsArraySize();
            final int projectileTrapsArraySize = calculateProjectileTrapsArraySize();
            final int animationsArraySize = calculateAnimationsArraySize();
            final int ambientsArraySize = calculateAmbientsArraySize();
            final int spawnPointsArraySize = calculateSpawnPointsArraySize();
            final int creatureArraySize = calculateCreatureArraySize();
            final int actorsArraySize = calculateActorsArraySize();
            final int verticesArraySize = calculateVerticesArraySize();
            final int doorsArraySize = calculateDoorsArraySize();
            final int regionsArraySize = calculateRegionsArraySize();
            final int itemsArraySize = calculateItemsArraySize();
            final int containersArraySize = calculateContainersArraySize();
            final int variablesArraySize = calculateVariablesArraySize();
            final int entrancesArraySize = calculateEntrancesArraySize();
            final int mapNotesArraySize = calculateMapNotesArraySize();

            final int total = headerSize + songEntriesSize + restInterruptionsSize + exploredBitmaskSize
                + projectileTrapEffectsArraySize + projectileTrapsArraySize + animationsArraySize + ambientsArraySize
                + spawnPointsArraySize + creatureArraySize + actorsArraySize + verticesArraySize + doorsArraySize
                + regionsArraySize + itemsArraySize + containersArraySize + variablesArraySize + entrancesArraySize
                + mapNotesArraySize;

            return new AreaSectionSizes(
                headerSize,
                songEntriesSize,
                restInterruptionsSize,
                exploredBitmaskSize,
                projectileTrapEffectsArraySize,
                projectileTrapsArraySize,
                animationsArraySize,
                ambientsArraySize,
                spawnPointsArraySize,
                creatureArraySize,
                actorsArraySize,
                verticesArraySize,
                doorsArraySize,
                regionsArraySize,
                itemsArraySize,
                containersArraySize,
                variablesArraySize,
                entrancesArraySize,
                mapNotesArraySize,
                total
            );
        }

        private int calculateHeaderSize()
        {
            return 0x11C;
        }

        private int calculateSongEntriesSize()
        {
            return 0x90;
        }

        private int calculateRestInterruptionsSize()
        {
            return 0xE4;
        }

        private int calculateExploredBitmaskSize()
        {
            return exploredBitmask.getUnimplemented().length;
        }

        private int calculateProjectileTrapEffectsArraySize()
        {
            // TODO: Unimplemented
            return 0x0;
        }

        private int calculateProjectileTrapsArraySize()
        {
            // TODO: Unimplemented
            return 0x0;
        }

        private int calculateAnimationsArraySize()
        {
            return 0x4C * animations.size();
        }

        private int calculateAmbientsArraySize()
        {
            return 0xD4 * ambients.size();
        }

        private int calculateSpawnPointsArraySize()
        {
            return 0xC8 * spawnPoints.size();
        }

        private int calculateCreatureArraySize()
        {
            // TODO: Unimplemented
            return 0x0;
        }

        private int calculateActorsArraySize()
        {
            return 0x110 * actors.size();
        }

        private int calculateVerticesArraySize()
        {
            for (final Container container : containers)
            {
                final short numVertices = (short)container.getPolygon().getVertices().size();
                numTotalVertices += numVertices;
            }

            doorVerticesFirstIndex = numTotalVertices;
            for (final Door door : doors)
            {
                final short numVertices = (short)(
                      door.getOpenOutlinePolygon().getVertices().size()
                    + door.getClosedOutlinePolygon().getVertices().size()
                    + door.getOpenImpededSearchMapCellsPolygon().getVertices().size()
                    + door.getClosedImpededSearchMapCellsPolygon().getVertices().size()
                );
                numTotalVertices += numVertices;
            }

            regionVerticesFirstIndex = numTotalVertices;
            for (final Region region : regions)
            {
                final short numVertices = (short)region.getPolygon().getVertices().size();
                numTotalVertices += numVertices;
            }

            return 0x4 * numTotalVertices;
        }

        private int calculateDoorsArraySize()
        {
            return 0xC8 * doors.size();
        }

        private int calculateRegionsArraySize()
        {
            return 0xC4 * regions.size();
        }

        private int calculateItemsArraySize()
        {
            for (final Container container : containers)
            {
                final short numContainerItems = (short)container.getItems().size();
                numTotalItems += numContainerItems;
            }

            return 0x14 * numTotalItems;
        }

        private int calculateContainersArraySize()
        {
            return 0xC0 * containers.size();
        }

        private int calculateVariablesArraySize()
        {
            return 0x54 * variables.size();
        }

        private int calculateEntrancesArraySize()
        {
            return 0x68 * entrances.size();
        }

        private int calculateMapNotesArraySize()
        {
            return 0x34 * mapNotes.size();
        }

        /////////////////////
        // Private Classes //
        /////////////////////

        // Vanilla ARE files (as marshalled by the game) are stored in the following order:
        //   Header
        //   Song Entries
        //   Rest Interruptions
        //   Explored Bitmask
        //   Projectile Trap Effects Array (pointed into by offset when a Projectile Trap entry has effects)
        //       (Projectile Trap 0)
        //       (Projectile Trap 1)
        //       ...
        //   Projectile Traps Array
        //       (Projectile Trap 0)
        //       (Projectile Trap 1)
        //       ...
        //   Animations Array
        //   Ambients Array
        //   Spawn Points Array
        //   Creature Array (pointed into by offset when an Actor entry embeds a CRE file)
        //        (Actor 0)
        //        (Actor 1)
        //        ...
        //   Actors Array
        //   Vertices Array
        //       (Container 0)
        //       (Container 1)
        //       ...
        //       (Door 0 open, Door 0 closed, Door 0 open impeded, Door 0 closed impeded)
        //       (Door 1 open, Door 1 closed, Door 1 open impeded, Door 1 closed impeded)
        //       ...
        //       (Region 0)
        //       (Region 1)
        //       ...
        //   Doors Array
        //   Regions Array
        //   Items Array
        //       (Container 0)
        //       (Container 1)
        //       ...
        //   Containers Array
        //   Variables Array
        //   Entrances Array
        //   Map Notes Array
        private record AreaSectionSizes(
            int headerSize,
            int songEntriesSize,
            int restInterruptionsSize,
            int exploredBitmaskSize,
            int projectileTrapEffectsArraySize,
            int projectileTrapsArraySize,
            int animationsArraySize,
            int ambientsArraySize,
            int spawnPointsArraySize,
            int creatureArraySize,
            int actorsArraySize,
            int verticesArraySize,
            int doorsArraySize,
            int regionsArraySize,
            int itemsArraySize,
            int containersArraySize,
            int variablesArraySize,
            int entrancesArraySize,
            int mapNotesArraySize,
            int total
        ) {}
    }

    private enum NO_INIT
    {
        DUMMY
    }
}
