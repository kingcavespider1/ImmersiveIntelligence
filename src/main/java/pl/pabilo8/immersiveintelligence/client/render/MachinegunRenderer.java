/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package pl.pabilo8.immersiveintelligence.client.render;

import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.common.util.ItemNBTHelper;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import pl.pabilo8.immersiveintelligence.Config.IIConfig.Weapons;
import pl.pabilo8.immersiveintelligence.ImmersiveIntelligence;
import pl.pabilo8.immersiveintelligence.client.model.weapon.ModelMachinegun;
import pl.pabilo8.immersiveintelligence.client.tmt.ModelRendererTurbo;
import pl.pabilo8.immersiveintelligence.client.tmt.TmtNamedBoxGroup;
import pl.pabilo8.immersiveintelligence.common.entity.EntityMachinegun;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class MachinegunRenderer extends Render<EntityMachinegun>
{
	public static final String texture = ImmersiveIntelligence.MODID+":textures/items/weapons/machinegun.png";
	public static ModelMachinegun model = new ModelMachinegun();
	public static HashMap<Predicate<ItemStack>, BiConsumer<ItemStack, List<TmtNamedBoxGroup>>> upgrades = new HashMap<>();
	public static List<TmtNamedBoxGroup> defaultGunParts = new ArrayList<>();

	public MachinegunRenderer(RenderManager renderManager)
	{
		super(renderManager);
		defaultGunParts.add(model.baseBox);
		defaultGunParts.add(model.barrelBox);
		defaultGunParts.add(model.sightsBox);
		defaultGunParts.add(model.triggerBox);
		defaultGunParts.add(model.ammoBox);
		defaultGunParts.add(model.slideBox);
		defaultGunParts.add(model.gripBox);
		defaultGunParts.add(model.bipodBox);
	}

	public static void renderMachinegun(ItemStack stack, @Nullable EntityMachinegun entity)
	{
		GlStateManager.pushMatrix();
		ClientUtils.bindTexture(texture);

		List<TmtNamedBoxGroup> renderParts = new ArrayList<>();
		renderParts.addAll(defaultGunParts);

		for(Entry<Predicate<ItemStack>, BiConsumer<ItemStack, List<TmtNamedBoxGroup>>> s : upgrades.entrySet())
		{
			if(s.getKey()!=null&&s.getValue()!=null&&s.getKey().test(stack))
				s.getValue().accept(stack, renderParts);
		}

		if(entity!=null)
		{
			float yaw = entity.gunYaw;
			if(entity.setupTime < 1&&entity.getPassengers().size() > 0&&entity.getPassengers().get(0) instanceof EntityLivingBase)
			{
				EntityLivingBase psg = (EntityLivingBase)entity.getPassengers().get(0);
				float true_head_angle = MathHelper.wrapDegrees(psg.rotationYawHead-entity.setYaw);

				if(entity.gunYaw < true_head_angle)
					yaw += ClientUtils.mc().getRenderPartialTicks()*2f;
				else if(entity.gunYaw > true_head_angle)
					yaw -= ClientUtils.mc().getRenderPartialTicks()*2f;

				if(Math.ceil(entity.gunYaw) <= Math.ceil(true_head_angle)+0.5f&&Math.ceil(entity.gunYaw) >= Math.ceil(true_head_angle)-0.5f)
					yaw = true_head_angle;

				yaw += entity.recoilYaw;
			}

			GlStateManager.translate(0f, -0.34375, 0f);

			for(TmtNamedBoxGroup nmod : renderParts)
			{
				ClientUtils.bindTexture(nmod.getTexturePath());
				if(nmod.getName().equals("bipod"))
				{
					GlStateManager.pushMatrix();
					GlStateManager.scale(0.85, 0.85, 0.85);
					GlStateManager.rotate(180-entity.setYaw, 0f, 1f, 0f);
					GlStateManager.translate(-0.5f, 0.34375, 1.65625);
					for(ModelRendererTurbo m : nmod.getModel())
						m.render(0.0625f);
					GlStateManager.popMatrix();
				}
				else
				{
					GlStateManager.pushMatrix();
					GlStateManager.scale(0.85, 0.85, 0.85);
					GlStateManager.rotate(180-entity.setYaw, 0f, 1f, 0f);
					GlStateManager.rotate(-yaw, 0f, 1f, 0f);
					GlStateManager.rotate(-entity.gunPitch, 1, 0, 0);
					GlStateManager.translate(-0.5f, 0.34375, 1.65625+(entity.gunPitch/20*0.25));

					if(nmod.getName().equals("ammo"))
					{
						boolean should_render = false;
						if(entity.currentlyLoaded==1)
						{

							float progress = entity.magazine1.isEmpty()?1f-Math.min(2*(float)entity.clipReload/(float)Weapons.machinegun.clipReloadTime, 1): (float)entity.clipReload/(float)Weapons.machinegun.clipReloadTime;
							GlStateManager.translate(0f, 0.375f*progress, 0f);
							should_render = true;
						}
						else if(!entity.magazine1.isEmpty())
							should_render = true;

						if(should_render)
							for(ModelRendererTurbo m : nmod.getModel())
								m.render(0.0625f);
					}
					else if(nmod.getName().equals("second_magazine_mag"))
					{
						boolean should_render = false;
						if(entity.currentlyLoaded==2)
						{

							float progress = entity.magazine2.isEmpty()?1f-Math.min(2*(float)entity.clipReload/(float)Weapons.machinegun.clipReloadTime, 1): (float)entity.clipReload/(float)Weapons.machinegun.clipReloadTime;
							GlStateManager.translate(0f, 0.375f*progress, 0f);
							should_render = true;
						}
						else if(!entity.magazine2.isEmpty())
							should_render = true;

						if(should_render)
							for(ModelRendererTurbo m : nmod.getModel())
								m.render(0.0625f);
					}
					else if(nmod.getName().equals("slide"))
					{
						if(((entity.currentlyLoaded==1&&entity.magazine1.isEmpty())||(entity.currentlyLoaded==2&&entity.magazine2.isEmpty()))&&((float)entity.clipReload/(float)Weapons.machinegun.clipReloadTime) > 0.5)
						{
							float curr = (((float)entity.clipReload/(float)Weapons.machinegun.clipReloadTime)-0.5f)/0.5f;
							float progress;
							if(curr > 0.65)
								progress = 1f-((curr-0.65f)/0.35f);
							else
								progress = (curr/0.65f);
							GlStateManager.translate(0f, 0f, progress*0.375);
						}
						for(ModelRendererTurbo m : nmod.getModel())
							m.render(0.0625f);
					}
					else
						for(ModelRendererTurbo m : nmod.getModel())
						{
							m.render(0.0625f);
						}
					GlStateManager.popMatrix();
				}
			}
		}
		else
		{
			for(TmtNamedBoxGroup nmod : renderParts)
			{
				ClientUtils.bindTexture(nmod.getTexturePath());

				if(nmod.getName().equals("ammo")&&!(ItemNBTHelper.hasKey(stack, "magazine1")&&!(new ItemStack(ItemNBTHelper.getTagCompound(stack, "magazine1")).isEmpty())))
					continue;
				if(nmod.getName().equals("second_magazine_mag")&&!(ItemNBTHelper.hasKey(stack, "magazine2")&&!(new ItemStack(ItemNBTHelper.getTagCompound(stack, "magazine2")).isEmpty())))
					continue;

				for(ModelRendererTurbo m : nmod.getModel())
					m.render(0.0625f);
			}
		}

		GlStateManager.popMatrix();
	}

	public static void drawBulletsList(ItemStack stack)
	{
		RenderHelper.enableGUIStandardItemLighting();
		GlStateManager.enableDepth();

		RenderItem ir = ClientUtils.mc().getRenderItem();

		if(ItemNBTHelper.hasKey(stack, "bullet0"))
		{
			ir.renderItemIntoGUI(new ItemStack(ItemNBTHelper.getTagCompound(stack, "bullet0")), 0, 0);
		}
		if(ItemNBTHelper.hasKey(stack, "bullet1"))
		{
			ir.renderItemIntoGUI(new ItemStack(ItemNBTHelper.getTagCompound(stack, "bullet1")), 0, 22);
		}
		if(ItemNBTHelper.hasKey(stack, "bullet2"))
		{
			ir.renderItemIntoGUI(new ItemStack(ItemNBTHelper.getTagCompound(stack, "bullet2")), 0, 44);
		}
		if(ItemNBTHelper.hasKey(stack, "bullet3"))
		{
			ir.renderItemIntoGUI(new ItemStack(ItemNBTHelper.getTagCompound(stack, "bullet3")), 0, 66);
		}

		GlStateManager.disableDepth();
	}

	/**
	 * Renders the desired {@code T} type Entity.
	 */
	@Override
	public void doRender(EntityMachinegun entity, double x, double y, double z, float f0, float f1)
	{

		GlStateManager.pushMatrix();
		GlStateManager.translate(x, y, z);
		GlStateManager.enableRescaleNormal();
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		RenderHelper.enableStandardItemLighting();

		if(entity.gun!=null&&!entity.gun.isEmpty())
			renderMachinegun(entity.gun, entity);


		GlStateManager.disableBlend();
		GlStateManager.disableRescaleNormal();
		GlStateManager.popMatrix();
	}

	/**
	 * Returns the location of an entity's texture. Doesn't seem to be called unless you call Render.bindEntityTexture.
	 */
	@Override
	protected ResourceLocation getEntityTexture(EntityMachinegun entity)
	{
		return new ResourceLocation(texture);
	}

}