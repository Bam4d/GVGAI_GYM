package qmul.gvgai.engine.ontology.effects.binary;

import lombok.extern.slf4j.Slf4j;
import qmul.gvgai.engine.core.vgdl.VGDLRegistry;
import qmul.gvgai.engine.core.vgdl.VGDLSprite;
import qmul.gvgai.engine.core.content.InteractionContent;
import qmul.gvgai.engine.core.game.Game;
import qmul.gvgai.engine.ontology.effects.unary.TransformTo;

import java.util.ArrayList;

@Slf4j
public class TransformIfCount extends TransformTo {

    //This effect transforms sprite1 into stype if
    // * num(stypeCount) >= GEQ
    // * num(stypeCount) <= LEQ
    public String stypeCount;
    public int itypeCount;
    public String estype;
    public int eitype;
    public int geq;
    public int leq;

    public TransformIfCount(InteractionContent cnt) throws Exception {
        super(cnt);
        geq = 0;
        leq = Game.getMaxSprites();
        this.parseParameters(cnt);
        itypeCount = VGDLRegistry.GetInstance().getRegisteredSpriteValue(stypeCount);

        if (estype != null)
            eitype = VGDLRegistry.GetInstance().getRegisteredSpriteValue(estype);
    }

    @Override
    public void execute(VGDLSprite sprite1, VGDLSprite sprite2, Game game) {
        if (sprite1 == null || sprite2 == null) {
            log.warn("Neither the 1st nor 2nd sprite can be EOS with TransformIfCount interaction.");
            return;
        }

        int numSpritesCheck = game.getNumSprites(itypeCount);
        this.applyScore = false;
        this.count = false;
        this.countElse = false;
        if (numSpritesCheck <= leq && numSpritesCheck >= geq) {
            VGDLSprite newSprite = game.addSprite(itype, sprite1.getPosition(), true);
            super.transformTo(newSprite, sprite1, sprite2, game);
            this.applyScore = true;
            this.count = true;
        } else if (estype != null) {
            VGDLSprite newSprite = game.addSprite(eitype, sprite1.getPosition(), true);
            super.transformTo(newSprite, sprite1, sprite2, game);
            this.countElse = true;
        }
    }

    @Override
    public ArrayList<String> getEffectSprites() {
        ArrayList<String> result = new ArrayList<String>();
        if (stype != null) result.add(stype);
        if (stypeCount != null) result.add(stypeCount);
        if (estype != null) result.add(estype);

        return result;
    }
}
