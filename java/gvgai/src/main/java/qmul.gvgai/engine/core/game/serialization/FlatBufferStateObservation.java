package qmul.gvgai.engine.core.game.serialization;

import com.google.flatbuffers.FlatBufferBuilder;
import qmul.gvgai.engine.core.game.Observation;
import qmul.gvgai.engine.core.game.StateObservation;
import qmul.gvgai.engine.ontology.Types;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static qmul.gvgai.engine.core.game.serialization.Observation.createObservation;

public class FlatBufferStateObservation {

    private FlatBufferBuilder b;

    private static List<String> actions;

    public boolean isValidation;

    static {
        actions = Arrays.asList(Action.names);
    }

    public FlatBufferStateObservation(StateObservation so, boolean includeSemanticData, byte[] tileArray, byte[] imageArray) {

        b = new FlatBufferBuilder(0);

        int imageArrayOffset = imageArray != null ? b.createByteVector(imageArray) : -1;
        int tileArrayOffset = tileArray != null ? b.createByteVector(tileArray) : -1;

        double[] worldDimensionVector = new double[2];
        worldDimensionVector[0] = so.getWorldDimension().width;
        worldDimensionVector[1] = so.getWorldDimension().height;

        int worldDimensionOffset = State.createWorldDimensionVector(b, worldDimensionVector);

        int availableActions = addAvailableActions(b, so);

        int avatarResources = 0;
        int observations = 0;
        int npcPositions = 0;
        int immovablePositions = 0;
        int movablePositions = 0;
        int resourcesPositions = 0;
        int fromAvatarSpritePositions = 0;
        int avatarPositionOffset = 0;
        int avatarOrientationVectorOffset = 0;

        if (includeSemanticData) {
            avatarResources = addAvatarResources(b, so);
            observations = addObservations(b, so);
            npcPositions = addNpcPositions(b, so);
            immovablePositions = addImmovablePositions(b, so);
            movablePositions = addMovablePositions(b, so);
            resourcesPositions = addResourcesPositions(b, so);
            fromAvatarSpritePositions = addFromAvatarSpritesPositions(b, so);
            avatarPositionOffset = convertVector(so.getAvatarPosition());
            avatarOrientationVectorOffset = convertVector(so.getAvatarOrientation());
        }

        State.startState(b);

        State.addIsValidation(b, isValidation);

        if (imageArray != null) {
            State.addImageArray(b, imageArrayOffset);
        }

        if (tileArray != null) {
            State.addTileArray(b, tileArrayOffset);
        }

        State.addIsValidation(b, false);
        State.addGameScore(b, so.getGameScore());
        State.addGameTick(b, so.getGameTick());
        State.addGameWinner(b, so.getGameWinner().ordinal());
        State.addIsGameOver(b, so.isGameOver());

        State.addWorldDimension(b, worldDimensionOffset);

        State.addBlockSize(b, so.getBlockSize());
        State.addNoOfPlayers(b, so.getNoPlayers());
        State.addAvatarSpeed(b, so.getAvatarSpeed());
        State.addAvatarLastAction(b, actions.indexOf(so.getAvatarLastAction().name()));
        State.addAvatarType(b, so.getAvatarType());
        State.addAvatarHealthPoints(b, so.getAvatarLimitHealthPoints());
        State.addAvatarMaxHealthPoints(b, so.getAvatarMaxHealthPoints());
        State.addIsAvatarAlive(b, so.isAvatarAlive());

        State.addAvailableActions(b, availableActions);

        if (includeSemanticData) {
            State.addAvatarResources(b, avatarResources);
            State.addObservationGrid(b, observations);
            State.addNPCPositions(b, npcPositions);
            State.addImmovablePositions(b, immovablePositions);
            State.addMovablePositions(b, movablePositions);
            State.addResourcesPositions(b, resourcesPositions);
            State.addFromAvatarSpritesPositions(b, fromAvatarSpritePositions);
            State.addAvatarOrientation(b, avatarOrientationVectorOffset);
            State.addAvatarPosition(b, avatarPositionOffset);
        }

        int end = State.endState(b);
        b.finish(end);
    }

    private int addAvatarResources(FlatBufferBuilder b, StateObservation so) {

        List<Integer> avatarResourceOffsets = so.getAvatarResources()
                .entrySet()
                .stream()
                .map(entry ->
                        IntKeyValuePair.createIntKeyValuePair(b, entry.getKey(), entry.getValue())
                ).collect(Collectors.toList());

        return State.createAvatarResourcesVector(b, convertListToNativeInt(avatarResourceOffsets));
    }

    private int addNpcPositions(FlatBufferBuilder b, StateObservation so) {
        List<Integer> npcPositionOffsets = process2DObservations(b, so.getNPCPositions());
        return State.createNPCPositionsVector(b, convertListToNativeInt(npcPositionOffsets));
    }

    private int addImmovablePositions(FlatBufferBuilder b, StateObservation so) {
        List<Integer> immovablePositionOffsets = process2DObservations(b, so.getImmovablePositions());
        return State.createImmovablePositionsVector(b, convertListToNativeInt(immovablePositionOffsets));
    }

    private int addMovablePositions(FlatBufferBuilder b, StateObservation so) {
        List<Integer> movablePositionOffsets = process2DObservations(b, so.getMovablePositions());
        return State.createMovablePositionsVector(b, convertListToNativeInt(movablePositionOffsets));
    }

    private int addResourcesPositions(FlatBufferBuilder b, StateObservation so) {
        List<Integer> resourcesPositionOffsets = process2DObservations(b, so.getResourcesPositions());
        return State.createResourcesPositionsVector(b, convertListToNativeInt(resourcesPositionOffsets));
    }

    private int addFromAvatarSpritesPositions(FlatBufferBuilder b, StateObservation so) {
        List<Integer> fromAvatarSpriteOffsets = process2DObservations(b, so.getFromAvatarSpritesPositions());
        return State.createResourcesPositionsVector(b, convertListToNativeInt(fromAvatarSpriteOffsets));
    }

    private List<Integer> process2DObservations(FlatBufferBuilder b, List<Observation>[] observations) {
        List<Integer> observationOffsets = new ArrayList<>();

        if (observations != null) {
            int rows = observations.length;
            for (int r = 0; r < rows; r++) {
                int cols = observations[r].size();
                for (int c = 0; c < cols; c++) {

                    Observation o = observations[r].get(c);

                    int referenceOffset = convertVector(o.reference);
                    int positionOffset = convertVector(o.position);

                    int obs = createObservation(
                            b,
                            o.category,
                            o.itype,
                            o.obsID,
                            referenceOffset,
                            positionOffset,
                            o.sqDist
                    );

                    observationOffsets.add(obs);
                }
            }
        }
        return observationOffsets;
    }

    private int addObservations(FlatBufferBuilder b, StateObservation so) {

        List<Integer> observations = new ArrayList<>();

        if (so.getObservationGrid() != null) {
            int zs = so.getObservationGrid().length;
            for (int z = 0; z < zs; z++) {
                int ys = so.getObservationGrid()[z].length;
                for (int y = 0; y < ys; y++) {
                    int cols = so.getObservationGrid()[z][y].size();
                    for (int c = 0; c < cols; c++) {

                        Observation o = so.getObservationGrid()[z][y].get(c);

                        int referenceOffset = convertVector(o.reference);
                        int positionOffset = convertVector(o.position);

                        int obs = createObservation(
                                b,
                                o.category,
                                o.itype,
                                o.obsID,
                                referenceOffset,
                                positionOffset,
                                o.sqDist
                        );

                        observations.add(obs);
                    }
                }
            }
        }

        return State.createMovablePositionsVector(b, convertListToNativeInt(observations));
    }

    private int addAvailableActions(FlatBufferBuilder b, StateObservation so) {
        List<Types.ACTIONS> availableActions = so.getAvailableActions();
        int[] availableActionsValues = new int[so.getAvailableActions().size()];

        for (int i = 0; i < availableActions.size(); i++) {
            availableActionsValues[i] = actions.indexOf(availableActions.get(i).name());
        }

        return State.createAvailableActionsVector(b, availableActionsValues);
    }

    private static int[] convertListToNativeInt(List<Integer> ints) {
        int[] intArray = new int[ints.size()];
        for (int i = 0; i < ints.size(); i++) {
            intArray[i] = ints.get(i);
        }
        return intArray;
    }

    private int convertVector(qmul.gvgai.engine.tools.Vector2d avatarPosition) {
        return Vector2d.createVector2d(b, avatarPosition.x, avatarPosition.y);
    }


    /***
     * This method serializes this class into bytes
     */
    public byte[] serialize() {
        return b.sizedByteArray();
    }

}
