package qmul.gvgai.engine.ontology.effects.binary;

import lombok.extern.slf4j.Slf4j;
import qmul.gvgai.engine.core.content.InteractionContent;
import qmul.gvgai.engine.core.game.Game;
import qmul.gvgai.engine.core.vgdl.VGDLSprite;
import qmul.gvgai.engine.ontology.effects.Effect;
import qmul.gvgai.engine.tools.Direction;
import qmul.gvgai.engine.tools.Vector2d;

import java.awt.*;
import java.util.ArrayList;

@Slf4j
public class WallReverse extends Effect {
    private double friction;
    private int lastGameTime;
    private ArrayList<VGDLSprite> spritesThisCycle;

    public WallReverse(InteractionContent cnt) {
        super.inBatch = true;
        lastGameTime = -1;
        spritesThisCycle = new ArrayList<>();
        this.parseParameters(cnt);
    }

    @Override
    public void execute(VGDLSprite sprite1, VGDLSprite sprite2, Game game) {
        if (sprite1 == null || sprite2 == null) {
            log.warn("Neither the 1st nor 2nd sprite can be EOS with WallReverse interaction.");
            return;
        }

        doReverse(sprite1, sprite2.rect, game);

        sprite1.setRect(sprite1.lastrect);
        sprite2.setRect(sprite2.lastrect);
    }

    public int executeBatch(VGDLSprite sprite1, ArrayList<VGDLSprite> sprite2list, Game game) {

        int nColls = super.sortBatch(sprite1, sprite2list, game);

        if (nColls == 1) {
            doReverse(sprite1, sprite2list.get(0).rect, game);
        } else {
            doReverse(sprite1, collision, game);
        }

        sprite1.setRect(sprite1.lastrect);
        for (VGDLSprite sprite2 : sprite2list)
            sprite2.setRect(sprite2.lastrect);

        return nColls;
    }

    private void doReverse(VGDLSprite sprite1, Rectangle s2rect, Game g) {
        boolean collisions[] = super.determineCollision(sprite1, s2rect, g);
        boolean horizontalBounce = collisions[0];
        boolean verticalBounce = collisions[1];


        Vector2d v;
        if (verticalBounce) {
            v = new Vector2d(sprite1.orientation.x(), 0);
        } else if (horizontalBounce) {
            v = new Vector2d(-sprite1.orientation.x(), 0);
        } else {
            //By default:
            v = new Vector2d(-sprite1.orientation.x(), 0);
        }

        double mag = v.mag();
        v.normalise();
        sprite1.orientation = new Direction(v.x, v.y);
        sprite1.speed = mag * sprite1.speed;
        if (sprite1.speed < sprite1.gravity) {
            sprite1.speed = sprite1.gravity;
        }


    }


}
