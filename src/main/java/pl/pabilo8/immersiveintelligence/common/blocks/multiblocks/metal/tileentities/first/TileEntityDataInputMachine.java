package pl.pabilo8.immersiveintelligence.common.blocks.multiblocks.metal.tileentities.first;

import blusunrize.immersiveengineering.ImmersiveEngineering;
import blusunrize.immersiveengineering.api.crafting.IMultiblockRecipe;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IAdvancedCollisionBounds;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IAdvancedSelectionBounds;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IGuiTile;
import blusunrize.immersiveengineering.common.blocks.metal.TileEntityMultiblockMetal;
import blusunrize.immersiveengineering.common.util.Utils;
import blusunrize.immersiveengineering.common.util.inventory.IEInventoryHandler;
import blusunrize.immersiveengineering.common.util.network.MessageTileSync;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import pl.pabilo8.immersiveintelligence.Config.IIConfig.Machines.DataInputMachine;
import pl.pabilo8.immersiveintelligence.api.data.DataPacket;
import pl.pabilo8.immersiveintelligence.api.data.IDataConnector;
import pl.pabilo8.immersiveintelligence.api.data.IDataDevice;
import pl.pabilo8.immersiveintelligence.api.utils.IBooleanAnimatedPartsBlock;
import pl.pabilo8.immersiveintelligence.common.CommonProxy;
import pl.pabilo8.immersiveintelligence.common.IIGuiList;
import pl.pabilo8.immersiveintelligence.common.items.ItemIIPunchtape;
import pl.pabilo8.immersiveintelligence.common.network.IIPacketHandler;
import pl.pabilo8.immersiveintelligence.common.network.MessageBooleanAnimatedPartsSync;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Pabilo8 on 28-06-2019.
 */
public class TileEntityDataInputMachine extends TileEntityMultiblockMetal<TileEntityDataInputMachine, IMultiblockRecipe> implements IDataDevice, IAdvancedCollisionBounds, IAdvancedSelectionBounds, IGuiTile, IBooleanAnimatedPartsBlock
{
	public boolean toggle = false;
	public float productionProgress = 0f;
	public DataPacket storedData = new DataPacket();
	public boolean isDrawerOpened = false, isDoorOpened = false;
	public float drawerAngle = 0, doorAngle = 0;
	public NonNullList<ItemStack> inventory = NonNullList.withSize(26, ItemStack.EMPTY);

	public TileEntityDataInputMachine()
	{
		super(MultiblockDataInputMachine.instance, new int[]{3, 2, 2}, DataInputMachine.energyCapacity, true);
	}

	@Override
	public void readCustomNBT(NBTTagCompound nbt, boolean descPacket)
	{
		super.readCustomNBT(nbt, descPacket);
		if(!descPacket&&!isDummy())
		{
			inventory = Utils.readInventory(nbt.getTagList("inventory", 10), 26);
			storedData.variables.clear();
			storedData.fromNBT(nbt.getCompoundTag("variables"));
			productionProgress = nbt.getFloat("production_progress");
		}
	}

	@Override
	public void onChunkUnload()
	{
		super.onChunkUnload();
	}

	@Override
	public void writeCustomNBT(NBTTagCompound nbt, boolean descPacket)
	{
		super.writeCustomNBT(nbt, descPacket);
		if(!descPacket&&!isDummy())
		{
			nbt.setTag("inventory", Utils.writeInventory(inventory));
			nbt.setTag("variables", storedData.toNBT());
			nbt.setFloat("production_progress", productionProgress);
		}
	}

	@Override
	public void receiveMessageFromClient(NBTTagCompound message)
	{
		super.receiveMessageFromClient(message);
		if(message.hasKey("variables"))
		{
			storedData.fromNBT(message.getCompoundTag("variables"));
		}
	}

	@Override
	public void receiveMessageFromServer(NBTTagCompound message)
	{
		super.receiveMessageFromServer(message);
		if(message.hasKey("variables"))
		{
			storedData.fromNBT(message.getCompoundTag("variables"));
		}
		if(message.hasKey("production_progress"))
		{
			productionProgress = message.getFloat("production_progress");
		}
	}

	@Override
	public void update()
	{
		super.update();

		if(world.isRemote&&pos==4)
		{
			if(isDrawerOpened&&drawerAngle < 5f)
				drawerAngle = Math.min(drawerAngle+0.4f, 5f);
			else if(!isDrawerOpened&&drawerAngle > 0f)
				drawerAngle = Math.max(drawerAngle-0.5f, 0f);

			if(isDoorOpened&&doorAngle < 135f)
				doorAngle = Math.min(doorAngle+3f, 135f);
			else if(!isDoorOpened&&doorAngle > 0f)
				doorAngle = Math.max(doorAngle-5f, 0f);


			if(this.productionProgress > 0&&energyStorage.getEnergyStored() > DataInputMachine.energyUsagePunchtape&&productionProgress < DataInputMachine.timePunchtapeProduction)
			{
				this.productionProgress += 1;
			}
		}

		if(!world.isRemote&&pos==4)
		{
			if(toggle^world.isBlockPowered(getBlockPosForPos(getRedstonePos()[0])))
			{
				toggle = !toggle;

				if(toggle)
				{
					this.onSend();
					//Finally!
					if(energyStorage.getEnergyStored() >= DataInputMachine.energyUsage)
					{
						energyStorage.extractEnergy(DataInputMachine.energyUsage, false);
						IDataConnector conn = pl.pabilo8.immersiveintelligence.api.Utils.findConnectorAround(getBlockPosForPos(3), this.world);
						if(conn!=null)
						{
							conn.sendPacket(storedData.clone());
						}
					}
				}
			}

			if((Utils.compareToOreName(inventoryHandler.getStackInSlot(0), "punchtapeEmpty")||inventoryHandler.getStackInSlot(0).getItem() instanceof ItemIIPunchtape)
					&&energyStorage.getEnergyStored() >= DataInputMachine.energyUsagePunchtape)
			{
				if(productionProgress==0||inventoryHandler.getStackInSlot(1).isEmpty())
				{
					ItemStack test = new ItemStack(CommonProxy.item_punchtape, 1, 0);

					((ItemIIPunchtape)test.getItem()).writeDataToItem(this.storedData, test);

					if(!inventoryHandler.insertItem(1, test, true).isEmpty())
						return;
				}
				energyStorage.extractEnergy(DataInputMachine.energyUsagePunchtape, false);
				productionProgress += 1;

				if(productionProgress >= DataInputMachine.timePunchtapeProduction)
				{
					productionProgress = 0f;
					ItemStack input = inventoryHandler.extractItem(0, 1, false);

					if(input.getItem() instanceof ItemIIPunchtape)
					{
						this.storedData = ((ItemIIPunchtape)input.getItem()).getStoredData(input);
						inventoryHandler.insertItem(1, input, false);

						NBTTagCompound nbt = new NBTTagCompound();
						nbt.setTag("inventory", Utils.writeInventory(inventory));
						nbt.setTag("variables", storedData.toNBT());
						ImmersiveEngineering.packetHandler.sendToAllAround(new MessageTileSync(this, nbt), pl.pabilo8.immersiveintelligence.api.Utils.targetPointFromPos(this.getPos(), this.world, 32));
					}
					else
					{
						ItemStack output = new ItemStack(CommonProxy.item_punchtape, 1, 0);

						((ItemIIPunchtape)output.getItem()).writeDataToItem(this.storedData, output);

						inventoryHandler.insertItem(1, output, false);

						NBTTagCompound nbt = new NBTTagCompound();
						nbt.setTag("inventory", Utils.writeInventory(inventory));
						ImmersiveEngineering.packetHandler.sendToAllAround(new MessageTileSync(this, nbt), pl.pabilo8.immersiveintelligence.api.Utils.targetPointFromPos(this.getPos(), this.world, 32));

					}

				}

				if(productionProgress==0||productionProgress==1||productionProgress==.5*DataInputMachine.energyUsagePunchtape)
				{
					NBTTagCompound tag = new NBTTagCompound();
					tag.setFloat("production_progress", productionProgress);
					ImmersiveEngineering.packetHandler.sendToAllAround(new MessageTileSync(this, tag), pl.pabilo8.immersiveintelligence.api.Utils.targetPointFromPos(this.getPos(), this.world, 32));
				}
			}
			else if(productionProgress!=0f)
			{
				productionProgress = 0f;
				NBTTagCompound tag = new NBTTagCompound();
				tag.setFloat("production_progress", productionProgress);
				ImmersiveEngineering.packetHandler.sendToAllAround(new MessageTileSync(this, tag), pl.pabilo8.immersiveintelligence.api.Utils.targetPointFromPos(this.getPos(), this.world, 32));
			}

		}
	}

	@Override
	public float[] getBlockBounds()
	{
		return new float[]{0, 0, 0, 0, 0, 0};
	}

	@Override
	public int[] getEnergyPos()
	{
		return new int[]{10};
	}

	@Override
	public int[] getRedstonePos()
	{
		return new int[]{2};
	}

	@Override
	public boolean isInWorldProcessingMachine()
	{
		return false;
	}

	@Override
	public void doProcessOutput(ItemStack output)
	{

	}

	@Override
	public void doProcessFluidOutput(FluidStack output)
	{
	}

	@Override
	public void onProcessFinish(MultiblockProcess<IMultiblockRecipe> process)
	{

	}

	@Override
	public int getMaxProcessPerTick()
	{
		return 1;
	}

	@Override
	public int getProcessQueueMaxLength()
	{
		return 1;
	}

	@Override
	public float getMinProcessDistance(MultiblockProcess<IMultiblockRecipe> process)
	{
		return 0;
	}

	@Override
	public NonNullList<ItemStack> getInventory()
	{
		return inventory;
	}

	@Override
	public boolean isStackValid(int slot, ItemStack stack)
	{
		return stack.getItem() instanceof ItemIIPunchtape;
	}

	@Override
	public int getSlotLimit(int slot)
	{
		return 26;
	}

	@Override
	public int[] getOutputSlots()
	{
		return new int[]{1};
	}

	@Override
	public int[] getOutputTanks()
	{
		return new int[]{};
	}

	@Override
	public boolean additionalCanProcessCheck(MultiblockProcess<IMultiblockRecipe> process)
	{
		return false;
	}

	@Override
	public IFluidTank[] getInternalTanks()
	{
		return new IFluidTank[]{};
	}

	@Override
	protected IFluidTank[] getAccessibleFluidTanks(EnumFacing side)
	{
		return new FluidTank[0];
	}

	@Override
	protected boolean canFillTankFrom(int iTank, EnumFacing side, FluidStack resource)
	{
		return false;
	}

	@Override
	protected boolean canDrainTankFrom(int iTank, EnumFacing side)
	{
		return false;
	}

	@Override
	public void doGraphicalUpdates(int slot)
	{
		this.markDirty();
		//this.markContainingBlockForUpdate(null);
	}

	@Override
	public IMultiblockRecipe findRecipeForInsertion(ItemStack inserting)
	{
		return null;
	}

	@Override
	protected IMultiblockRecipe readRecipeFromNBT(NBTTagCompound tag)
	{
		return null;
	}

	@Override
	public void onSend()
	{

	}

	@Override
	public void onReceive(DataPacket packet, EnumFacing side)
	{
		/*if (this.pos==3 && energyStorage.getEnergyStored()>=dataInputMachine.energyUsage)
		{
			energyStorage.extractEnergy(dataInputMachine.energyUsage,false);
		}*/
	}

	@Override
	public List<AxisAlignedBB> getAdvancedSelectionBounds()
	{
		List list = new ArrayList<AxisAlignedBB>();

		if(pos==0)
		{
			list.add(new AxisAlignedBB(1d/16d, 0, 1d/16d, 4d/16d, 13d/16d, 4d/16d).offset(getPos().getX(), getPos().getY(), getPos().getZ()));
		}
		else
			list.add(new AxisAlignedBB(0, 0, 0, 1, 1, 1).offset(getPos().getX(), getPos().getY(), getPos().getZ()));

		return list;
	}

	@Override
	public boolean isOverrideBox(AxisAlignedBB box, EntityPlayer player, RayTraceResult mop, ArrayList<AxisAlignedBB> list)
	{
		return false;
	}

	@Override
	public List<AxisAlignedBB> getAdvancedColisionBounds()
	{
		return getAdvancedSelectionBounds();
	}

	@Override
	public boolean canOpenGui()
	{
		return true;
	}

	@Override
	public int getGuiID()
	{
		return IIGuiList.GUI_DATA_INPUT_MACHINE_STORAGE;
	}

	@Nullable
	@Override
	public TileEntity getGuiMaster()
	{
		return master();
	}

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing)
	{
		if(capability==CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
			return master()!=null;
		return super.hasCapability(capability, facing);
	}

	IItemHandler inventoryHandler = new IEInventoryHandler(26, this, 0, true, true);

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing)
	{
		if(capability==CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
		{
			TileEntityDataInputMachine master = master();
			if(master==null)
				return null;
			return (T)this.inventoryHandler;
		}
		return super.getCapability(capability, facing);
	}

	@Override
	public void onGuiOpened(EntityPlayer player, boolean clientside)
	{
		if(!clientside)
		{
			NBTTagCompound tag = new NBTTagCompound();
			tag.setTag("variables", storedData.toNBT());
			ImmersiveEngineering.packetHandler.sendToAllAround(new MessageTileSync(this, tag), new TargetPoint(this.world.provider.getDimension(), this.getPos().getX(), this.getPos().getY(), this.getPos().getZ(), 32));
		}
	}

	@Override
	public void onAnimationChangeClient(boolean state, int part)
	{
		if(part==0)
			isDrawerOpened = state;
		else if(part==1)
			isDoorOpened = state;
	}

	@Override
	public void onAnimationChangeServer(boolean state, int part)
	{
		if(part==0)
			isDrawerOpened = state;
		else if(part==1)
			isDoorOpened = state;

		IIPacketHandler.INSTANCE.sendToAllAround(new MessageBooleanAnimatedPartsSync(isDrawerOpened, 0, getPos()), pl.pabilo8.immersiveintelligence.api.Utils.targetPointFromPos(this.getPos(), this.world, 32));
		IIPacketHandler.INSTANCE.sendToAllAround(new MessageBooleanAnimatedPartsSync(isDoorOpened, 1, getPos()), pl.pabilo8.immersiveintelligence.api.Utils.targetPointFromPos(this.getPos(), this.world, 32));

	}
}
