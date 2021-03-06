package qmul.gvgai.engine.ontology.effects.binary;

import qmul.gvgai.engine.core.vgdl.VGDLRegistry;
import qmul.gvgai.engine.core.vgdl.VGDLSprite;
import qmul.gvgai.engine.core.content.InteractionContent;
import qmul.gvgai.engine.core.game.Game;
import qmul.gvgai.engine.ontology.effects.Effect;

import java.util.ArrayList;
import java.util.Iterator;


public class SetSpeedForAll extends Effect
{
    public String stype; // sets the speed to value for all sprites of type stype
    public int itype;
    public double value=0;

    public SetSpeedForAll(InteractionContent cnt) throws Exception
    {
        is_stochastic = true;
        this.parseParameters(cnt);
        itype = VGDLRegistry.GetInstance().getRegisteredSpriteValue(stype);
        if(itype == -1){
            throw new Exception("Undefined sprite " + stype);
        }
    }

    @Override
    public void execute(VGDLSprite sprite1, VGDLSprite sprite2, Game game)
    {

        ArrayList<Integer> subtypes = game.getSubTypes(itype);
        for (Integer i: subtypes) {
            Iterator<VGDLSprite> spriteIt = game.getSpriteGroup(i);
            if (spriteIt != null) while (spriteIt.hasNext()) {
                try {
                    VGDLSprite s = spriteIt.next();
                    s.speed = value;
                } catch (ClassCastException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
