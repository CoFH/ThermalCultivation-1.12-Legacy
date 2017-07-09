package cofh.thermalcultivation.block;

import cofh.core.block.BlockCore;
import cofh.core.render.IModelRegister;
import cofh.core.util.core.IInitializer;
import cofh.thermalcultivation.ThermalCultivation;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.EnumPlantType;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.UseHoeEvent;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

public class BlockSoil extends BlockCore implements IInitializer, IModelRegister {

	protected static final AxisAlignedBB FARMLAND_AABB = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.9375D, 1.0D);
	public static final PropertyEnum<Type> VARIANT = PropertyEnum.create("type", Type.class);
	public static final PropertyBool TILLED = PropertyBool.create("tilled");

	public BlockSoil() {

		super(Material.GROUND, "thermalcultivation");

		setUnlocalizedName("soil");
		setCreativeTab(ThermalCultivation.tabCommon);

		setSoundType(SoundType.GROUND);
		setTickRandomly(true);
		setLightOpacity(255);
	}

	@Override
	protected BlockStateContainer createBlockState() {

		return new BlockStateContainer(this, VARIANT, TILLED);
	}

	@Override
	public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> items) {

		for (int i = 0; i < Type.METADATA_LOOKUP.length; i++) {
			items.add(new ItemStack(this, 1, i));
		}
	}

	/* TYPE METHODS */
	@Override
	public IBlockState getStateFromMeta(int meta) {

		return getDefaultState().withProperty(VARIANT, Type.byMetadata(meta & 7)).withProperty(TILLED, meta > 7);
	}

	@Override
	public int getMetaFromState(IBlockState state) {

		return state.getValue(VARIANT).getMetadata() + (state.getValue(TILLED) ? 8 : 0);
	}

	@Override
	public int damageDropped(IBlockState state) {

		return state.getValue(VARIANT).getMetadata();
	}

	/* BLOCK METHODS */
	@Override
	public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {

		if (!hasCrop(world, pos)) {
			return;
		}
		switch(state.getValue(VARIANT)) {
			case BASIC:
				break;
			case RICH:
				if (world.rand.nextInt(100) < 25) {
					break;
				}
			case FLUX:
				if (world.rand.nextInt(100) < 25) {
					break;
				}
				BlockPos up = pos.up();
				world.scheduleBlockUpdate(up, world.getBlockState(up).getBlock(), 0, 1);
		}
	}

	@Override
	public boolean canSustainPlant(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing direction, net.minecraftforge.common.IPlantable plantable) {

		IBlockState plant = plantable.getPlant(world, pos.offset(direction));
		EnumPlantType plantType = plantable.getPlantType(world, pos.offset(direction));

		switch (plantType) {
			case Plains:
			case Desert:
			case Beach:
				return true;
			case Crop:
				return state.getValue(TILLED);
			default:
				return false;
		}
	}

	@Override
	public boolean isFertile(World world, BlockPos pos) {

		return world.getBlockState(pos).getValue(TILLED);
	}

	@Override
	public boolean isFullCube(IBlockState state) {

		return !state.getValue(TILLED);
	}

	@Override
	public boolean isOpaqueCube(IBlockState state) {

		return !state.getValue(TILLED);
	}

	@Override
	public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {

		return state.getValue(VARIANT).getLight();
	}

	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {

		return state.getValue(TILLED) ? FARMLAND_AABB : FULL_BLOCK_AABB;
	}

	@Override
	@SideOnly (Side.CLIENT)
	public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {

	}

	protected boolean hasCrop(World world, BlockPos pos) {

		Block block = world.getBlockState(pos.up()).getBlock();
		return block instanceof IPlantable && canSustainPlant(world.getBlockState(pos), world, pos, EnumFacing.UP, (IPlantable) block);
	}

	/* IModelRegister */
	@Override
	@SideOnly (Side.CLIENT)
	public void registerModels() {

		for (int i = 0; i < Type.values().length; i++) {
			ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(this), i, new ModelResourceLocation(modName + ":" + name, "tilled=false,type=" + Type.byMetadata(i).getName()));
		}
	}

	/* EVENT HANDLING */
	@SubscribeEvent
	public void handleUseHoeEvent(UseHoeEvent event) {

		World world = event.getWorld();
		IBlockState state = world.getBlockState(event.getPos());

		if (state.getBlock() == this) {
			if (!state.getValue(TILLED)) {
				world.setBlockState(event.getPos(), state.withProperty(TILLED, true));
				event.setResult(Result.ALLOW);
			} else {
				event.setCanceled(true);
			}
		}
	}

	/* IInitializer */
	@Override
	public boolean initialize() {

		this.setRegistryName("soil");
		ForgeRegistries.BLOCKS.register(this);

		ItemBlockSoil itemBlock = new ItemBlockSoil(this);
		itemBlock.setRegistryName(this.getRegistryName());
		ForgeRegistries.ITEMS.register(itemBlock);

		MinecraftForge.EVENT_BUS.register(this);

		ThermalCultivation.proxy.addIModelRegister(this);

		return true;
	}

	@Override
	public boolean register() {

		return true;
	}

	/* TYPE */
	public enum Type implements IStringSerializable {

		// @formatter:off
		BASIC(0, "basic"),
		RICH(1, "rich"),
		FLUX(2, "flux", 7, EnumRarity.UNCOMMON);
		// MANA(3, "mana", 10, EnumRarity.RARE);
		// @formatter: on

		private static final Type[] METADATA_LOOKUP = new Type[values().length];
		private final int metadata;
		private final String name;
		private final int light;
		private final EnumRarity rarity;

		Type(int metadata, String name, int light, EnumRarity rarity) {

			this.metadata = metadata;
			this.name = name;
			this.light = light;
			this.rarity = rarity;
		}

		Type(int metadata, String name, int light) {

			this(metadata, name, light, EnumRarity.COMMON);
		}

		Type(int metadata, String name) {

			this(metadata, name, 0, EnumRarity.COMMON);
		}

		public int getMetadata() {

			return this.metadata;
		}

		@Override
		public String getName() {

			return this.name;
		}

		public int getLight() {

			return this.light;
		}

		public EnumRarity getRarity() {

			return this.rarity;
		}

		public static Type byMetadata(int metadata) {

			if (metadata < 0 || metadata >= METADATA_LOOKUP.length) {
				metadata = 0;
			}
			return METADATA_LOOKUP[metadata];
		}

		static {
			for (Type type : values()) {
				METADATA_LOOKUP[type.getMetadata()] = type;
			}
		}
	}

	/* REFERENCES */
	public static ItemStack soilBasic;
	public static ItemStack soilRich;
	public static ItemStack soilFlux;

}
