package glengine.yan.glengine.nodes;

import glengine.yan.glengine.assets.YANTextureRegion;
import glengine.yan.glengine.assets.font.YANFont;
import glengine.yan.glengine.assets.font.YANFontChar;
import glengine.yan.glengine.programs.YANTextShaderProgram;

import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.glDrawArrays;

/**
 * Created by Yan-Home on 10/26/2014.
 */
public class YANTextNode extends YANBaseNode<YANTextShaderProgram> {

    private static final int VERTICES_COUNT_FOR_ONE_CHAR = 6;

    /**
     * Text that will be rendered
     */
    private String mText;

    /**
     * Font that is used to render text
     */
    private YANFont mFont;

    /**
     * vertex data is a cached float array with a fixed size.
     * that array changing when the text is changing , it will always have only position and texture coordinates data.
     */
    private float[] mVertexData;


    public YANTextNode(YANFont font) {
        mFont = font;

        //text should never be empty !
        mText = " ";
    }

    @Override
    protected float[] createVertexData() {

        //TODO : consider this implementation for featured text rendering
        //https://github.com/ShaRose/GuiAPI/blob/master/twl/src/de/matthiasmann/twl/renderer/lwjgl/BitmapFont.java

        // here we are creating a mesh with indices and texture coordinates for the entire text
        // that way we can render it in one render call.
        // According to rendering guide http://www.angelcode.com/products/bmfont/doc/render_text.html

        //we must render the amount of characters according to a length of the text
        mVertexData = new float[VALUES_PER_VERTEX * VERTICES_COUNT_FOR_ONE_CHAR * mText.length()];

        //TODO : need to consider a size of the node somehow ?

        //we use this value to calculate the offset in data float array
        int numElementsForOneCharRendering = VALUES_PER_VERTEX * VERTICES_COUNT_FOR_ONE_CHAR;

        //data array offset
        int arrOffset = 0;

        //cursor is used to calculate character position
        int cursorPositionX = 0;
        int kerning;

        YANFontChar currChar;

        //go over the entire text and get chars from font
        for (int i = 0; i < mText.length(); i++) {

            //we are checking kernings from second element and on
            kerning = ((i > 0)) ? mFont.getKerningValueForChars(mText.charAt(i - 1), mText.charAt(i)) : 0;

            //convert char to ascii value
            char chr = mText.charAt(i);

            //try to obtain a char form the loaded font object
            currChar = mFont.getCharsMap().get((int) chr);
            if (currChar == null) {
                throw new RuntimeException("Character " + chr + " is not found !");
            }

            //move cursor by advance value
            cursorPositionX += kerning;

            //now we are filling a data array to store rendering information for the character
            loadDataForTextureRegion(currChar.getWidth(), currChar.getHeight(), currChar.getYANTextureRegion(), arrOffset,
                    cursorPositionX + currChar.getXOffset(), 0);

            //move cursor by advance value
            cursorPositionX += currChar.getXAdvance();

            //update float array offset
            arrOffset += numElementsForOneCharRendering;

            //TODO : handle line breaks ?
        }

        return mVertexData;
    }

    private void loadDataForTextureRegion(float width, float height, YANTextureRegion sampleTextureRegion, int arrOffset, int offsetX, int offsetY) {
        // Order of coordinates: X, Y, U, V
        // Triangle Strip

        // vertex (top left)
        mVertexData[arrOffset + 0] = offsetX;
        mVertexData[arrOffset + 1] = offsetY + height;
        mVertexData[arrOffset + 2] = sampleTextureRegion.getU0();
        mVertexData[arrOffset + 3] = sampleTextureRegion.getV1();

        // vertex (bottom right)
        mVertexData[arrOffset + 4] = offsetX + width;
        mVertexData[arrOffset + 5] = offsetY;
        mVertexData[arrOffset + 6] = sampleTextureRegion.getU1();
        mVertexData[arrOffset + 7] = sampleTextureRegion.getV0();

        // vertex (bottom left)
        mVertexData[arrOffset + 8] = offsetX;
        mVertexData[arrOffset + 9] = offsetY;
        mVertexData[arrOffset + 10] = sampleTextureRegion.getU0();
        mVertexData[arrOffset + 11] = sampleTextureRegion.getV0();

        // vertex (top left)
        mVertexData[arrOffset + 12] = offsetX;
        mVertexData[arrOffset + 13] = offsetY + height;
        mVertexData[arrOffset + 14] = sampleTextureRegion.getU0();
        mVertexData[arrOffset + 15] = sampleTextureRegion.getV1();

        // vertex (top right)
        mVertexData[arrOffset + 16] = offsetX + width;
        mVertexData[arrOffset + 17] = offsetY + height;
        mVertexData[arrOffset + 18] = sampleTextureRegion.getU1();
        mVertexData[arrOffset + 19] = sampleTextureRegion.getV1();

        // vertex (bottom right)
        mVertexData[arrOffset + 20] = offsetX + width;
        mVertexData[arrOffset + 21] = offsetY;
        mVertexData[arrOffset + 22] = sampleTextureRegion.getU1();
        mVertexData[arrOffset + 23] = sampleTextureRegion.getV0();
    }

    @Override
    public void bindData(YANTextShaderProgram shaderProgram) {
        vertexArray.setVertexAttribPointer(
                0,
                shaderProgram.getPositionAttributeLocation(),
                POSITION_COMPONENT_COUNT,
                STRIDE);

        vertexArray.setVertexAttribPointer(
                POSITION_COMPONENT_COUNT,
                shaderProgram.getTextureCoordinatesAttributeLocation(),
                TEXTURE_COORDINATES_COMPONENT_COUNT,
                STRIDE);

        //TODO : maybe color ?
    }

    public void setText(String text) {
        mText = text;

        if (text == null || text.length() == 0) {
            //text should never be empty !
            mText = " ";
        }

        //every time text changes , data must be recalculated
        recalculateDimensions();
    }

    public void setFont(YANFont font) {
        mFont = font;
    }

    public YANFont getFont() {
        return mFont;
    }

    @Override
    protected void onRender() {
        //do the draw
        glDrawArrays(GL_TRIANGLES, 0, mVertexData.length / VALUES_PER_VERTEX);
    }

    public String getText() {
        return mText;
    }
}