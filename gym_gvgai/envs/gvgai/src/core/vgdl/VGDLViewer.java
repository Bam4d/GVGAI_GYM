package core.vgdl;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import core.game.Game;
import core.player.LearningPlayer;
import core.player.Player;
import ontology.Types;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.IntBuffer;
import java.util.ArrayList;


/**
 * Created with IntelliJ IDEA.
 * User: Diego
 * Date: 24/10/13
 * Time: 10:54
 * This is a Java port from Tom Schaul's VGDL - https://github.com/schaul/py-vgdl
 */
public class VGDLViewer extends JComponent {
    /**
     * Reference to the game to be painted.
     */
    public Game game;

    /**
     * Dimensions of the window.
     */
    private Dimension size;

    /**
     * Sprites to draw
     */
    public SpriteGroup[] spriteGroups;

    /**
     * Player of the game
     */
    public Player player;

    public boolean justImage = false;

//    BufferedImage texture;
//
//    Graphics2D graphics;

    // OpenGL Frame buffer
    private FrameBuffer frameBuffer;

    private static GL20 gl;

    static {
        gl = Gdx.gl;
    }


    /**
     * Creates the viewer for the game.
     *
     * @param game game to be displayed
     */
    public VGDLViewer(Game game, Player player) {
        this.game = game;
        this.size = game.getScreenSize();
        this.player = player;

        if (player instanceof LearningPlayer) {

            if (((LearningPlayer) player).isRequiresImage()) {



                gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

                frameBuffer = new FrameBuffer(Pixmap.Format.RGB888, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false);


//                BufferedImage bi = new BufferedImage((int) size.getWidth(), (int) size.getHeight(), BufferedImage.TYPE_INT_RGB);
//
//                // obtain the current system graphical settings
//                GraphicsConfiguration gfxConfig = GraphicsEnvironment.
//                        getLocalGraphicsEnvironment().getDefaultScreenDevice().
//                        getDefaultConfiguration();
//
//                // texture is not optimized, so create a new texture that is
//                texture = gfxConfig.createCompatibleImage(
//                        bi.getWidth(), bi.getHeight(), bi.getTransparency());
//
//                // get the graphics context of the new texture to draw the old texture on
//                graphics = texture.createGraphics();

                updateObservationForLearningPlayer();
            }
        }
    }

    private void updateObservationForLearningPlayer() {
        LearningPlayer learningPlayer = (LearningPlayer) player;
        Texture texture = paintFrameBuffer();

        learningPlayer.setObservation(texture.getTextureData().consumePixmap().getPixels().array());
    }

    /**
     * Main method to paint the game
     *
     * @param gx Graphics object.
     */
    public void paintComponent(Graphics gx) {
        // TODO: WORK OUT HOW TO DO THIS WITH OPENGL
        //paintWithGraphics(g);
    }

    public Texture paintFrameBuffer() {
        //For a better graphics, enable this: (be aware this could bring performance issues depending on your HW & OS).
        //g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        //g.setColor(Types.LIGHTGRAY);

        frameBuffer.begin();
        {
            gl.glClearColor(1f,0,0,1f);
            gl.glClear(GL20.GL_COLOR_BUFFER_BIT);


            SpriteBatch spriteBatch = new SpriteBatch();

            spriteBatch.begin();

            int[] gameSpriteOrder = game.getSpriteOrder();
            if (this.spriteGroups != null) for (Integer spriteTypeInt : gameSpriteOrder) {
                if (spriteGroups[spriteTypeInt] != null) {
                    ArrayList<VGDLSprite> spritesList = spriteGroups[spriteTypeInt].getSprites();
                    for (VGDLSprite sp : spritesList) {
                        if (sp != null) sp.draw(spriteBatch, game);
                    }

                }
            }

            spriteBatch.end();

//            player.draw(g);
        }
        frameBuffer.end();

        return frameBuffer.getColorBufferTexture();
    }


    /**
     * Paints the sprites.
     *
     * @param spriteGroupsGame sprites to paint.
     */
    public void paint(SpriteGroup[] spriteGroupsGame) {

        if (player instanceof LearningPlayer) {

            if (!((LearningPlayer) player).isRequiresImage()) {
                return;
            }

            this.spriteGroups = spriteGroupsGame;
//            for (int i = 0; i < this.spriteGroups.length; ++i) {
//                this.spriteGroups[i] = new SpriteGroup(spriteGroupsGame[i].getItype());
//                this.spriteGroups[i].copyAllSprites(spriteGroupsGame[i].getSprites());
//            }

            updateObservationForLearningPlayer();
        } else {
            this.repaint();
        }
    }

    /**
     * Gets the dimensions of the window.
     *
     * @return the dimensions of the window.
     */
    public Dimension getPreferredSize() {
        return size;
    }

}
