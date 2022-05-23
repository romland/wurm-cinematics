package com.wurmonline.client.renderer.gui;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.lwjgl.opengl.Display;

import com.friya.wurmonline.client.hollywurm.Mod;
import com.friya.wurmonline.client.hollywurm.ScriptRunner;
import com.friya.wurmonline.client.hollywurm.WurmHelpers;
import com.wurmonline.client.options.Option;
import com.wurmonline.client.renderer.Matrix;
import com.wurmonline.client.renderer.backend.Primitive;
import com.wurmonline.client.renderer.backend.Primitive.BlendMode;
import com.wurmonline.client.renderer.backend.Queue;
import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
import com.wurmonline.client.renderer.gui.Renderer;
import com.wurmonline.client.renderer.gui.text.TextFont;
import com.wurmonline.client.stats.Stats;

/*
to render in-game text on coordinate, see:

wurmonline/client/renderer/cell/GroundItemCellRenderable.java
    public void createAttachedTexts() {


        if (this.item.getModelName().toString().contains("sign.large")) {
            final TextQuad textQuad = new TextQuad(this.item.getDescription(), TextFont.getSignText(), new Vector2f(1.0f, 0.6f), new Vector3f(0.02f, 1.65f, 0.042f));
            this.attachedTextQuads.add(textQuad);
        }   

get an item id and attach it on load

 */


public class BenchmarkRenderer extends Renderer
{
	private static Logger logger = Logger.getLogger(Mod.class.getName());

	private final TextFont text;
	private final HeadsUpDisplay hud;

	private static Matrix modelMatrix;
	
	private static float currentFadeAlpha = 0f;
	private static float targetFadeAlpha = 0f;
	private static boolean fading = false;

	public BenchmarkRenderer(HeadsUpDisplay hud) 
	{
		super(hud);

		this.text = TextFont.getAchievementText();
		this.hud = hud;
		
		BenchmarkRenderer.modelMatrix = new Matrix();
	}
/*	
	public boolean isRendering()
	{
		if(jobToken != -1) {
		}
	}
*/
	public static void fadeToBlack()
	{
		logger.info("fade to black");

		currentFadeAlpha = 0f;
		targetFadeAlpha = 1f;
		fading = true;
	}
	
	public static void fadeToShow()
	{
		logger.info("fade to show");

		currentFadeAlpha = 1f;
		targetFadeAlpha = 0f;
		fading = true;
	}
	
	public static boolean isFading()
	{
		return fading;
	}
	
    @Override
    public void execute(final Object arg) 
    {
        final Queue queue = (Queue)arg;
//        logger.info("Am I called?");

       	renderFader(queue);

       	if(ScriptRunner.isBenchmarking()) {
       		renderBenchmarkInfo(queue, 10, 10);
       	}
    }

    private void renderFader(final Queue queue)
    {
    	//if(fading == false || (targetFadeAlpha > currentFadeAlpha && currentFadeAlpha > 0.99999f) || (targetFadeAlpha < currentFadeAlpha && currentFadeAlpha < 0.00001f)) {
       	if(currentFadeAlpha == targetFadeAlpha && currentFadeAlpha != 1f) {
			fading = false;
			return;
    	}

    	final Primitive bg = queue.reservePrimitive();
		bg.type = Primitive.Type.TRIANGLESTRIP;

		bg.blendmode = BlendMode.ALPHABLEND;
		
		bg.num = 2;
		bg.vertex = Primitive.staticVertexSquare2D;
		bg.index = null;
		bg.texture[0] = (bg.texture[1] = null);
		bg.r = 0f;
		bg.g = 0f;
		bg.b = 0f;

		float lerp = 0.25f;
		float deltaTime = 0.3f;		// TODO
		bg.a = currentFadeAlpha + ((targetFadeAlpha - currentFadeAlpha) * lerp * deltaTime);

		if(Math.abs(targetFadeAlpha - currentFadeAlpha) < 0.001f) {
			currentFadeAlpha = targetFadeAlpha;
		} else {
			currentFadeAlpha = bg.a;
		}

//		logger.info("fading: " + BenchmarkRenderer.isFading() + " " + currentFadeAlpha);
		
		bg.clipRect = HeadsUpDisplay.scissor.getCurrent();

        BenchmarkRenderer.modelMatrix.fromTranslationAndNonUniformScale(0, 0, 0.0f, Display.getWidth(), Display.getHeight(), 0.0f);
        queue.queue(bg, BenchmarkRenderer.modelMatrix);
    }

    
    private void renderBenchmarkInfo(final Queue queue, final int xMousePos, final int yMousePos)
    {
		final int maxAllowedWidth = Display.getWidth();
		final int lineHeight = this.text.getHeight();
		final int descent = this.text.getDescent();
		int lineCount = 0;
		int maxWidth = 0;
//        final boolean isDev = this.hud.getWorld().getServerConnection().isDev() || Options.USE_DEV_DEBUG;
        final List<String> linesToDraw = new LinkedList<String>();

        List<String> lines = new ArrayList<String>(); //this.pickData.getText();
        
        lines.add("Benchmarking with Friya's Cinematics");
        if(Mod.showScriptInfoWhenBenchmarking) {
			lines.add("Pending commands: " + ScriptRunner.getPendingTweenCount());
//			lines.add("Scene duration: " + (float)(ScriptRunner.getSceneElapsedTime() / 1000f));
			lines.add("Scene duration: " + (float)(ScriptRunner.getSceneElapsedTimeExcludingSetup() / 1000f));
        }
		lines.add("Duration: " + (float)(ScriptRunner.getScriptElapsedTime() / 1000f));
		lines.add("FPS: " + Stats.fps.get());

        for (final String line : lines) {
            final int lineWidth = this.text.getWidth(line);

            if (lineWidth > maxAllowedWidth) {
                final List<String> subdivided = new LinkedList<String>();
                this.subdivide(subdivided, line, maxAllowedWidth, 0);
                for (final String sub : subdivided) {
                    linesToDraw.add(sub);
                    final int subLineWidth = this.text.getWidth(sub);
                    if (subLineWidth > maxWidth) {
                        maxWidth = subLineWidth;
                    }
                }
            } else {
                linesToDraw.add(line);
                if (lineWidth <= maxWidth) {
                    continue;
                }

                maxWidth = lineWidth;
            }
        }

        lineCount = linesToDraw.size();
        if (lineCount == 0) {
            return;
        }

        final int width = this.hud.getWidth();
        final int height = this.hud.getHeight();
        final int w = maxWidth;
        final int h = lineHeight * lineCount;
        int x = xMousePos + 4;
        int y = yMousePos + lineHeight;

        if (x + w + 4 > width) {
            x = width - w - 4;
        }

        if (y + h + 4 > height) {
            y = yMousePos - h - 4;
        }

        int h2 = this.text.getDescent();
        this.text.moveTo(x, y + h2);

        for (final String line2 : linesToDraw) {
            h2 += lineHeight;
            this.text.moveTo(x, y + h2);
            this.text.paint(queue, line2, 1.0f, 1.0f, 1.0f, 1.0f);
        }
    }
    
    void foo()
    {
/*
		final Primitive bg = queue.reservePrimitive();
		bg.type = Primitive.Type.TRIANGLESTRIP;
		bg.num = 2;
		bg.vertex = Primitive.staticVertexSquare2D;
		bg.index = null;
		bg.texture[0] = (bg.texture[1] = null);
		bg.r = 0.132f;
		bg.g = 0.157f;
		bg.b = 0.322f;
		bg.a = 0.75f;

		bg.clipRect = HeadsUpDisplay.scissor.getCurrent();

        BenchmarkRenderer.modelMatrix.fromTranslationAndNonUniformScale(x - 2, y + descent, 0.0f, w + 4, h, 0.0f);
        queue.queue(bg, BenchmarkRenderer.modelMatrix);
*/

    }
    

    private void subdivide(final List<String> output, final String line, final int maxAllowedWidth, final int startIndex)
    {
        final int len = line.length();
        final char[] chars = line.toCharArray();
        float measuredLength = 0.0f;
        int measuredUntil = startIndex;
        int pos = startIndex;
        while (measuredUntil < len) {
            while (pos < len && line.charAt(pos) != ' ') {
                ++pos;
            }
            measuredLength += this.text.getWidth(chars, measuredUntil, pos - measuredUntil);
            final int previousMeasure = measuredUntil;
            measuredUntil = pos;
            if (measuredLength > maxAllowedWidth) {
                if (startIndex != previousMeasure) {
                    pos = previousMeasure;
                    break;
                }
                pos = startIndex + maxAllowedWidth / this.text.getWidth(".");
                if (pos > chars.length) {
                    pos = chars.length;
                    break;
                }
                break;
            }
            else {
                while (pos < len && line.charAt(pos) == ' ') {
                    ++pos;
                }
            }
        }
        final String segment = ((startIndex > 0) ? "    " : "") + new String(chars, startIndex, pos - startIndex);
        output.add(segment);
        while (pos < len && chars[pos] == ' ') {
            ++pos;
        }
        if (pos < len) {
            this.subdivide(output, line, maxAllowedWidth, pos);
        }
    }


    /*
	Map<String, List<OptionWidget>> options = OptionWidget.getOptionsMap();
	for(Map.Entry<String,List<OptionWidget>> entry : options.entrySet()) {
	    String key = entry.getKey();
	    List<OptionWidget> vals = entry.getValue();
	    
		logger.info(key);
		for(OptionWidget val : vals) {
			
			logger.info("\t" + val.getLabel().getText() + ": " + val.getComponent().getName());
		}
	}
*/

    // picturing this:
    // https://static.giantbomb.com/uploads/original/2/29874/1830083-untitled.jpg
    public static void dumpOptionValues() 
    {
    	/*
    	{
    		"display_settings",
    		"water_detail",
    		"reflections	",
    		"terrain_res",
    		"trees",
    		"structure_render_distance",
    		"item_creature_render_distance",
    		"tiledecorations",
    		"skydetail",
    		"render_distant_terrain",
    		"terrain_bump",
    		"use_antialiasing",
    		"use_anisotropic_filtering",
    		"max_texture_size",
    		"reflection_texture_size",
    		"offscreen_texture_size",
    		"mega_texture_size",
    		"hint_texture_scaling",
    		"enable_lod",
    		"lod",
    		"use_tree_models",
    		"limit_dynamic_lights",
    		"max_dynamic_lights",
    		"use_weather_particles",
    		"use_non_alpha_particles",
    		"use_alpha_particles",
    		"glsl_enabled",
    		"vbo_enabled",
    		"fbo_enabled",
    		"multidraw_enabled",
    		"auto_mipmaps_enabled",
    		"occlusion_queries_enabled",
    		"depth_clamp_enabled",
    		"shadow_mapsize",
    		"shadow_level",
    		"render_bloom",
    		"render_vignette",
    		"render_fxaa",
    		"fov_horizontal",
    		"fps_limit_background",
    		"fps_limit",
    		"fps_limit_enabled",
    		"enable_vsync",
    		"gpu_skinning",
    		"max_shader_lights"
    	};
    	*/
        final Option[] options = WurmHelpers.getWurmOptions().values().toArray(new Option[0]);
        logger.info("" + options[0]);
        for (int i = 1; i < options.length; ++i) {
            logger.info("" + options[i]);
        }   
    }   

}
