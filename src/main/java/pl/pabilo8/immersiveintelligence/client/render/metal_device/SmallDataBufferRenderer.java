package pl.pabilo8.immersiveintelligence.client.render.metal_device;

import blusunrize.immersiveengineering.client.ClientUtils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import pl.pabilo8.immersiveintelligence.ImmersiveIntelligence;
import pl.pabilo8.immersiveintelligence.client.model.metal_device.ModelSmallDataBuffer;
import pl.pabilo8.immersiveintelligence.common.blocks.metal.TileEntitySmallDataBuffer;

/**
 * Created by Pabilo8 on 2019-05-26.
 */
@SideOnly(Side.CLIENT)
public class SmallDataBufferRenderer extends TileEntitySpecialRenderer<TileEntitySmallDataBuffer>
{
	private static ModelSmallDataBuffer model = new ModelSmallDataBuffer();

	private static String texture = ImmersiveIntelligence.MODID+":textures/blocks/metal_device/small_data_buffer.png";

	@Override
	public void render(TileEntitySmallDataBuffer te, double x, double y, double z, float partialTicks, int destroyStage, float alpha)
	{
		if(te!=null)
		{
			ClientUtils.bindTexture(texture);
			GlStateManager.pushMatrix();
			GlStateManager.translate(x+1, y, z);
			GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);

			model.getBlockRotation(te.facing, model);
			model.render();

			GlStateManager.popMatrix();
			return;

		}
		else
		{

			GlStateManager.pushMatrix();
			GlStateManager.translate(x+1, y, z);
			GlStateManager.enableBlend();
			GlStateManager.enableAlpha();

			ClientUtils.bindTexture(texture);
			GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);

			model.getBlockRotation(EnumFacing.NORTH, model);
			model.render();

			GlStateManager.popMatrix();
			return;
		}
	}
}