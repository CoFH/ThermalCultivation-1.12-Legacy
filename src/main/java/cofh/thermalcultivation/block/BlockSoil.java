package cofh.thermalcultivation.block;

import cofh.core.block.BlockCore;
import cofh.core.render.IModelRegister;
import cofh.core.util.core.IInitializer;
import cofh.thermalcultivation.ThermalCultivation;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockSoil extends BlockCore implements IInitializer, IModelRegister {

	protected static final AxisAlignedBB FARMLAND_AABB = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.9375D, 1.0D);
	public static final PropertyEnum<Type> VARIANT = PropertyEnum.create("type", Type.class);

	public BlockSoil() {

		super(Material.GROUND, "thermalcultivation");

		setUnlocalizedName("soil");
		setCreativeTab(ThermalCultivation.tabCommon);

		setTickRandomly(true);
		setLightOpacity(255);
	}

	/* TYPE METHODS */
	@Override
	public IBlockState getStateFromMeta(int meta) {

		return this.getDefaultState().withProperty(VARIANT, Type.byMetadata(meta));
	}

	@Override
	public int getMetaFromState(IBlockState state) {

		return state.getValue(VARIANT).getMetadata();
	}

	@Override
	public int damageDropped(IBlockState state) {

		return state.getValue(VARIANT).getMetadata();
	}

	@Override
	public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {

		return Type.byMetadata(state.getBlock().getMetaFromState(state)).light;
	}

	/* IModelRegister */
	@Override
	@SideOnly (Side.CLIENT)
	public void registerModels() {

		for (int i = 0; i < Type.values().length; i++) {
			ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(this), i, new ModelResourceLocation(modName + ":" + name, "type=" + Type.byMetadata(i).getName()));
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
