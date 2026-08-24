package com.example.iknow.item;

import com.example.iknow.DeIntegration;
import com.example.iknow.ModDataComponents;
import com.example.iknow.PickupMode;
import com.example.iknow.ToolMode;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.common.ItemAbilities;

/**
 * 澶氬姛鑳藉伐鍏凤細闆嗘枾澶淬€侀晲瀛愩€侀攧澶淬€佸壀鍒€銆侀摬瀛愪簬涓€韬€? * - 宸ュ叿妯″紡锛堝彲澶氶€夛級浠ヤ綅鎺╃爜瀛樺湪 {@link ModDataComponents#TOOL_MODES}
 * - 闄勯瓟妯″紡锛堝彧鍙崟閫夛細绮惧噯閲囬泦 / 鏃惰繍锛夊瓨鍦?{@link ModDataComponents#ENCHANT_MODE}
 * - 鑷甫 50 鐐规敾鍑讳激瀹炽€佹姠澶?X銆佽櫄鎷熼檮榄旓紙绮惧噯閲囬泦/鏃惰繍锛夛紝鎷ユ湁鏃跺彲鍒涢€犻琛? */
public class IknowToolItem extends Item {

    // ============ 闄勯瓟妯″紡甯搁噺 ============
        public static final int ENCHANT_OFF = 0;
    public static final int ENCHANT_SILK = 1;
    public static final int ENCHANT_FORTUNE = 2;

    // ============ 鎸栨帢閫熷害锛堥捇鐭崇骇 脳5锛岃秴蹇級 ============
        public static final float AXE_SPEED = 40.0F;
    public static final float PICKAXE_SPEED = 40.0F;
    public static final float HOE_SPEED = 40.0F;
    public static final float SHEARS_SPEED = 20.0F;
    public static final float SHOVEL_SPEED = 40.0F;

    public IknowToolItem(Properties properties) {
        super(properties);
    }

    /** 璇诲彇璇ョ墿鍝佹爤褰撳墠鍚敤鐨勫伐鍏锋ā寮忔帺鐮?*/
    public static int modes(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.TOOL_MODES.get(), ToolMode.DEFAULT_MASK);
    }

    public static boolean isModeEnabled(ItemStack stack, ToolMode mode) {
        return ToolMode.isEnabled(modes(stack), mode);
    }

    /** 璇诲彇璇ョ墿鍝佹爤褰撳墠鐨勯檮榄旀ā寮?*/
    public static int enchantMode(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ENCHANT_MODE.get(), ENCHANT_OFF);
    }

    /** 璇诲彇璇ョ墿鍝佹爤鐨勬寲鎺橀€熷害婊戝潡鍊硷紙0-100锛岄粯璁?50锛?*/
    public static int miningSpeed(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.MINING_SPEED.get(), 50);
    }

    /** 璇诲彇璇ョ墿鍝佹爤鐨勯琛岄€熷害婊戝潡鍊硷紙0-100锛岄粯璁?50 = 鍘熺増閫熷害锛?*/
    public static int flightSpeed(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.FLIGHT_SPEED.get(), 50);
    }

    // ============ 瑙﹀強璺濈 / 鏀诲嚮璺濈锛堥粯璁?鍘熺増榛樿鍊硷紝鏈€楂?10锛?============
        public static final float DEFAULT_BLOCK_REACH = 4.5F;
    public static final float DEFAULT_ATTACK_REACH = 3.0F;

    /** 璇诲彇鏂瑰潡瑙﹀強璺濈 */
    public static float blockReach(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.BLOCK_REACH.get(), DEFAULT_BLOCK_REACH);
    }

    /** 璇诲彇鏀诲嚮璺濈 */
    public static float attackReach(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ATTACK_REACH.get(), DEFAULT_ATTACK_REACH);
    }

    /** 璇诲彇璇ョ墿鍝佹爤鐨勬嬀鍙栨ā寮忥紙榛樿鍏抽棴锛?*/
    public static PickupMode magnetMode(ItemStack stack) {
        int v = stack.getOrDefault(ModDataComponents.PICKUP_MODE.get(), 0);
        int m = v & 0x0F;
        return switch (m) {
            case 1 -> PickupMode.MAGNET;
            case 2 -> PickupMode.MAGNET_AE;
            default -> PickupMode.NONE;
        };
    }

    /** 读取该物品的破坏轴模式（NONE / BREAK_INVENTORY / BREAK_AE），默认关闭 */
    public static PickupMode breakMode(ItemStack stack) {
        int v = stack.getOrDefault(ModDataComponents.PICKUP_MODE.get(), 0);
        int b = (v >> 4) & 0x0F;
        return switch (b) {
            case 1 -> PickupMode.BREAK_INVENTORY;
            case 2 -> PickupMode.BREAK_AE;
            default -> PickupMode.NONE;
        };
    }

    /** 写入磁吸轴 + 破坏轴（两个独立选项，可同时启用） */
    public static void setPickupModes(ItemStack stack, PickupMode magnet, PickupMode brk) {
        int magVal = switch (magnet) {
            case MAGNET -> 1;
            case MAGNET_AE -> 2;
            default -> 0;
        };
        int brkVal = switch (brk) {
            case BREAK_INVENTORY -> 1;
            case BREAK_AE -> 2;
            default -> 0;
        };
        stack.set(ModDataComponents.PICKUP_MODE.get(), magVal | (brkVal << 4));
    }

    /** 兼容读取：优先磁吸轴，其次破坏轴（无则关闭） */
    public static PickupMode pickupMode(ItemStack stack) {
        PickupMode magnet = magnetMode(stack);
        return magnet != PickupMode.NONE ? magnet : breakMode(stack);
    }

    // ============ 鎸栨帢鐩稿叧锛堟墍鏈夋柟鍧椾竴寰嬫寜婊戝潡閫熷害锛屾棤鎸栨帢鎯╃綒锛?============

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        // 婊戝潡 0-100锛岄粯璁?50 瀵瑰簲閫熷害 40锛涜秺澶ц秺蹇?
        return Math.max(0.5F, 40.0F * miningSpeed(stack) / 50.0F);
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        // 鎭掍负姝ｇ‘宸ュ叿锛氬彇娑?闇€瑕佹纭伐鍏?鏂瑰潡鐨?5 鍊嶆寲鎺樻儵缃氾紝涓旀墍鏈夋柟鍧楁甯告帀钀?
        return true;
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
        // 鍓戠被妯壂鏀诲嚮锛氬儚鍓戜竴鏍峰彲妯壂
        if (itemAbility == ItemAbilities.SWORD_SWEEP) {
            return true;
        }
        int modes = modes(stack);
        if (ToolMode.isEnabled(modes, ToolMode.AXE) && ItemAbilities.DEFAULT_AXE_ACTIONS.contains(itemAbility)) {
            return true;
        }
        if (ToolMode.isEnabled(modes, ToolMode.PICKAXE) && ItemAbilities.DEFAULT_PICKAXE_ACTIONS.contains(itemAbility)) {
            return true;
        }
        if (ToolMode.isEnabled(modes, ToolMode.HOE) && ItemAbilities.DEFAULT_HOE_ACTIONS.contains(itemAbility)) {
            return true;
        }
        if (ToolMode.isEnabled(modes, ToolMode.SHOVEL) && ItemAbilities.DEFAULT_SHOVEL_ACTIONS.contains(itemAbility)) {
            return true;
        }
        return ToolMode.isEnabled(modes, ToolMode.SHEARS) && ItemAbilities.DEFAULT_SHEARS_ACTIONS.contains(itemAbility);
    }

    // ============ 鍙抽敭浜や簰锛堝墺鐨?鍒攬/鍘昏湣/鑰曞湴/閾茶矾/鐏伀/鍓垁绫伙級 ============

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        // 娼滆+鍙抽敭缁戝畾 AE 鏃犵嚎璁块棶鐐瑰凡绉昏嚦 PickupEvents#onRightClickBlock锛?
        // 鍥犱负鍦ㄦ柟鍧楄 AE 鎵虫墜娑堣垂鍓嶅繀椤诲厛鎷︽埅锛寀seOn 鍒颁笉浜嗚繖閲屻€?
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        int modes = modes(context.getItemInHand());

        List<ItemAbility> abilities = new ArrayList<>();
        if (ToolMode.isEnabled(modes, ToolMode.AXE)) {
            abilities.add(ItemAbilities.AXE_STRIP);
            abilities.add(ItemAbilities.AXE_SCRAPE);
            abilities.add(ItemAbilities.AXE_WAX_OFF);
        }
        if (ToolMode.isEnabled(modes, ToolMode.HOE)) {
            abilities.add(ItemAbilities.HOE_TILL);
        }
        if (ToolMode.isEnabled(modes, ToolMode.SHOVEL)) {
            abilities.add(ItemAbilities.SHOVEL_FLATTEN);
            abilities.add(ItemAbilities.SHOVEL_DOUSE);
        }
        if (ToolMode.isEnabled(modes, ToolMode.SHEARS)) {
            abilities.add(ItemAbilities.SHEARS_CARVE);
            abilities.add(ItemAbilities.SHEARS_HARVEST);
            abilities.add(ItemAbilities.SHEARS_TRIM);
            abilities.add(ItemAbilities.SHEARS_DISARM);
        }

        for (ItemAbility ability : abilities) {
            BlockState modified = state.getToolModifiedState(context, ability, false);
            if (modified != null && modified != state) {
                if (!level.isClientSide) {
                    level.setBlock(pos, modified, 11);
                    playToolSound(level, pos, ability);
                    if (player != null) {
                        context.getItemInHand().hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
                    }
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return InteractionResult.PASS;
    }

    private static void playToolSound(Level level, BlockPos pos, ItemAbility ability) {
        if (ability == ItemAbilities.AXE_STRIP) {
            level.playSound(null, pos, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 1.0F, 1.0F);
        } else if (ability == ItemAbilities.AXE_SCRAPE) {
            level.playSound(null, pos, SoundEvents.AXE_SCRAPE, SoundSource.BLOCKS, 1.0F, 1.0F);
        } else if (ability == ItemAbilities.AXE_WAX_OFF) {
            level.playSound(null, pos, SoundEvents.AXE_WAX_OFF, SoundSource.BLOCKS, 1.0F, 1.0F);
        } else if (ability == ItemAbilities.HOE_TILL) {
            level.playSound(null, pos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0F, 1.0F);
        } else if (ability == ItemAbilities.SHOVEL_FLATTEN) {
            level.playSound(null, pos, SoundEvents.SHOVEL_FLATTEN, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    // ============ 瀹炰綋浜や簰锛堝壀鍒€鍓緤姣涳級 ============

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity entity, InteractionHand hand) {
        if (isModeEnabled(stack, ToolMode.SHEARS) && entity instanceof Sheep sheep && sheep.isAlive() && !sheep.isSheared()) {
            if (!player.level().isClientSide) {
                sheep.shear(SoundSource.PLAYERS);
                stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            }
            return InteractionResult.sidedSuccess(player.level().isClientSide);
        }
        return InteractionResult.PASS;
    }

    // ============ 鏀诲嚮鍛戒腑锛氬彲鐩存帴鍑绘潃娣锋矊榫欙紙榫欎箣鐮旂┒锛?============

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, net.minecraft.world.entity.Entity entity) {
        if (DeIntegration.tryKillGuardian(entity)) {
            return true;
        }
        return super.onLeftClickEntity(stack, player, entity);
    }

    // ============ 鏀诲嚮灞炴€э紙50 鐐逛激瀹筹級 ============

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers() {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        // 鍩虹鏀诲嚮 1.0 + 65 = 66 鐐逛激瀹?
        new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 65.0, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        // 4.0 - 0.8 = 3.2 鏀婚€?= 閽荤煶鍓?1.6) 鐨?2 鍊?
        new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -0.8, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    // ============ 闄勯瓟鏁堟灉锛堥€夋嫨闄勯瓟妯″紡鏃跺啓鍏ョ湡瀹為檮榄旓紝淇濊瘉鎵€鏈夎矾寰勭敓鏁堬級 ============

    /**
     * 鏍规嵁闄勯瓟妯″紡鎶婄湡瀹為檮榄斿啓鍏ョ墿鍝侊細
     * - 濮嬬粓甯?鎶㈠ず X
     * - 绮惧噯閲囬泦锛氬姞 绮惧噯閲囬泦 I
     * - 鏃惰繍锛氬姞 鏃惰繍 X
     * 鐪熷疄闄勯瓟鍚屾椂浣滅敤浜庢柟鍧楁帀钀藉垽瀹氾紙match_tool 鎴樺埄鍝佹潯浠讹級涓庡嚮鏉€鎺夎惤锛屽苟闄勫甫闄勯瓟鍏夋晥銆?     */
    public static void applyEnchantments(ItemStack stack, int enchantMode, RegistryAccess registryAccess) {
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(stack.getEnchantments());
        Holder<Enchantment> silk = holder(registryAccess, Enchantments.SILK_TOUCH);
        Holder<Enchantment> fortune = holder(registryAccess, Enchantments.FORTUNE);
        Holder<Enchantment> looting = holder(registryAccess, Enchantments.LOOTING);
        mutable.set(silk, 0);
        mutable.set(fortune, 0);
        mutable.set(looting, 0);
        mutable.set(looting, 10);
        if (enchantMode == ENCHANT_SILK) {
            mutable.set(silk, 1);
        }
        if (enchantMode == ENCHANT_FORTUNE) {
            mutable.set(fortune, 10);
        }
        stack.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());
    }

    private static Holder<Enchantment> holder(RegistryAccess registryAccess, ResourceKey<Enchantment> key) {
        return registryAccess.holder(key).orElseThrow();
    }

    // ============ 鎮诞鎻愮ず ============

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        List<String> modeNames = new ArrayList<>();
        for (ToolMode mode : ToolMode.values()) {
            if (ToolMode.isEnabled(modes(stack), mode)) {
                modeNames.add(mode.displayName().getString());
            }
        }
        tooltipComponents.add(Component.translatable("tooltip.iknow.tool_modes")
                .withStyle(ChatFormatting.GRAY)
                .append(String.join("、", modeNames)));
        tooltipComponents.add(Component.translatable("tooltip.iknow.enchant_mode")
                .withStyle(ChatFormatting.GRAY)
                .append(enchantModeName(stack)));
        tooltipComponents.add(Component.translatable("tooltip.iknow.desc1").withStyle(ChatFormatting.DARK_GRAY));
        tooltipComponents.add(Component.translatable("tooltip.iknow.desc2").withStyle(ChatFormatting.DARK_GRAY));
        tooltipComponents.add(Component.translatable("tooltip.iknow.desc3").withStyle(ChatFormatting.AQUA));
    }

    private static Component enchantModeName(ItemStack stack) {
        return switch (enchantMode(stack)) {
            case ENCHANT_SILK -> Component.translatable("enchantmode.iknow.silk");
            case ENCHANT_FORTUNE -> Component.translatable("enchantmode.iknow.fortune");
            default -> Component.translatable("enchantmode.iknow.off");
        };
    }
}

