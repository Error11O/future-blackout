package com.example.addon.modules.anarchy;

import com.example.addon.Addon;
import com.example.addon.modules.utils.OLEPOSSUtils;
import meteordevelopment.meteorclient.events.entity.EntityAddedEvent;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.DamageUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.List;

/*
Made by OLEPOSSU / Raksamies
*/

public class AutoCrystal extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPlace = settings.createGroup("Place");
    private final SettingGroup sgExplode = settings.createGroup("Explode");
    private final SettingGroup sgMisc = settings.createGroup("Misc");
    private final SettingGroup sgDamage = settings.createGroup("Damage");
    private final SettingGroup sgRotate = settings.createGroup("Rotate");
    private final SettingGroup sgRaytrace = settings.createGroup("Raytrace");
    private final SettingGroup sgRender = settings.createGroup("Render");

    //  General Page

    private final Setting<Boolean> place = sgGeneral.add(new BoolSetting.Builder()
        .name("Place")
        .description(".")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> explode = sgGeneral.add(new BoolSetting.Builder()
        .name("Explode")
        .description(".")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> eatPause = sgGeneral.add(new BoolSetting.Builder()
        .name("Pause When Eating")
        .description(".")
        .defaultValue(true)
        .build()
    );
    private final Setting<ListenerMode> calcMode = sgGeneral.add(new EnumSetting.Builder<ListenerMode>()
        .name("Calculation Mode")
        .description(".")
        .defaultValue(ListenerMode.TickPre)
        .build()
    );

    //  Place Page
    private final Setting<Boolean> superSmartRangeChecks = sgGeneral.add(new BoolSetting.Builder()
        .name("Super Smart Range Checks")
        .description(".")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> placeRange = sgPlace.add(new DoubleSetting.Builder()
        .name("Place Range")
        .description(".")
        .defaultValue(6)
        .range(0, 10)
        .sliderMax(10)
        .build()
    );
    private final Setting<Boolean> placeSwing = sgPlace.add(new BoolSetting.Builder()
        .name("Place Swing")
        .description(".")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> prePlaceSwing = sgPlace.add(new BoolSetting.Builder()
        .name("Pre Place Swing")
        .description(".")
        .defaultValue(true)
        .build()
    );
    private final Setting<SwingMode> placeSwingMode = sgPlace.add(new EnumSetting.Builder<SwingMode>()
        .name("Place Swing Mode")
        .description(".")
        .defaultValue(SwingMode.Full)
        .build()
    );

    //  Explode Page

    private final Setting<Double> explodeRange = sgExplode.add(new DoubleSetting.Builder()
        .name("Break Range")
        .description(".")
        .defaultValue(6)
        .range(0, 10)
        .sliderMax(10)
        .build()
    );
    private final Setting<Double> explodeWalls = sgExplode.add(new DoubleSetting.Builder()
        .name("Break Walls Range")
        .description(".")
        .defaultValue(3)
        .range(0, 10)
        .sliderMax(10)
        .build()
    );
    private final Setting<Boolean> onlyExplodeWhenHolding = sgExplode.add(new BoolSetting.Builder()
        .name("Only Explode When Holding")
        .description(".")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> setDead = sgExplode.add(new BoolSetting.Builder()
        .name("SetDead")
        .description("Removes crystals.")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> explodeBlocking = sgExplode.add(new BoolSetting.Builder()
        .name("Explode Blocking")
        .description(".")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> alwaysExplodeBlocking = sgExplode.add(new BoolSetting.Builder()
        .name("Always Explode Blocking")
        .description(".")
        .defaultValue(true)
        .visible(explodeBlocking::get)
        .build()
    );
    private final Setting<Boolean> rangeIgnBlocking = sgExplode.add(new BoolSetting.Builder()
        .name("Ignore Range When Blocking")
        .description(".")
        .defaultValue(false)
        .visible(explodeBlocking::get)
        .build()
    );
    private final Setting<Boolean> damageIgnBlocking = sgExplode.add(new BoolSetting.Builder()
        .name("Ignore Damage When Blocking")
        .description(".")
        .defaultValue(true)
        .visible(explodeBlocking::get)
        .build()
    );
    private final Setting<Boolean> explodeSwing = sgExplode.add(new BoolSetting.Builder()
        .name("Explode Swing")
        .description(".")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> preExplodeSwing = sgExplode.add(new BoolSetting.Builder()
        .name("Pre Explode Swing")
        .description(".")
        .defaultValue(true)
        .build()
    );
    private final Setting<SwingMode> explodeSwingMode = sgExplode.add(new EnumSetting.Builder<SwingMode>()
        .name("Explode Swing Mode")
        .description(".")
        .defaultValue(SwingMode.Full)
        .build()
    );

    //  Misc Page
    private final Setting<Boolean> idPredict = sgMisc.add(new BoolSetting.Builder()
        .name("ID Predict")
        .description("Attacks crystals before they spawn.")
        .defaultValue(false)
        .build()
    );
    private final Setting<Double> idDelay = sgMisc.add(new DoubleSetting.Builder()
        .name("ID Packet Delay")
        .description(".")
        .defaultValue(0.05)
        .range(0, 1)
        .sliderMax(1)
        .build()
    );

    private final Setting<Boolean> allowOffhand = sgMisc.add(new BoolSetting.Builder()
        .name("Allow Offhand")
        .description("Removes crystals Instantly after spawning.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> preferMainHand = sgMisc.add(new BoolSetting.Builder()
        .name("Prefer Mainhand")
        .description("Removes crystals Instantly after spawning.")
        .defaultValue(false)
        .visible(allowOffhand::get)
        .build()
    );

    //  Damage Page
    private final Setting<Boolean> deadCheck = sgDamage.add(new BoolSetting.Builder()
        .name("Dead Check")
        .description("Ignores dead player in calculations")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> forcePop = sgDamage.add(new IntSetting.Builder()
        .name("Force Pop")
        .description(".")
        .defaultValue(2)
        .range(0, 10)
        .sliderMax(10)
        .build()
    );
    private final Setting<Integer> antiPop = sgDamage.add(new IntSetting.Builder()
        .name("Anti Pop")
        .description(".")
        .defaultValue(1)
        .range(0, 10)
        .sliderMax(10)
        .build()
    );
    private final Setting<Boolean> suicidal = sgDamage.add(new BoolSetting.Builder()
        .name("Suicidal")
        .description("Attacks if both pop")
        .defaultValue(false)
        .build()
    );
    private final Setting<Double> minHealthLeft = sgDamage.add(new DoubleSetting.Builder()
        .name("Min Health Left")
        .description(".")
        .defaultValue(4)
        .range(0, 36)
        .sliderMax(36)
        .build()
    );
    private final Setting<Double> minPlaceDamage = sgDamage.add(new DoubleSetting.Builder()
        .name("Min Place Damage")
        .description(".")
        .defaultValue(5)
        .range(0, 20)
        .sliderMax(20)
        .build()
    );
    private final Setting<Double> maxSelfPlace = sgDamage.add(new DoubleSetting.Builder()
        .name("Max Place Damage")
        .description(".")
        .defaultValue(0.7)
        .range(0, 10)
        .sliderMax(10)
        .build()
    );
    private final Setting<Boolean> ignoreBreak = sgDamage.add(new BoolSetting.Builder()
        .name("Ignore Damage")
        .description("Ignores Break Damage")
        .defaultValue(false)
        .build()
    );
    private final Setting<Double> minExplodeDamage = sgDamage.add(new DoubleSetting.Builder()
        .name("Min Explode Damage")
        .description(".")
        .defaultValue(5)
        .range(0, 20)
        .sliderMax(20)
        .visible(() -> !ignoreBreak.get())
        .build()
    );
    private final Setting<Double> maxSelfBreak = sgDamage.add(new DoubleSetting.Builder()
        .name("Max Explode Damage")
        .description(".")
        .defaultValue(0.7)
        .range(0, 10)
        .sliderMax(10)
        .visible(() -> !ignoreBreak.get())
        .build()
    );

    //  Rotate Page

    private final Setting<Boolean> rotate = sgRotate.add(new BoolSetting.Builder()
        .name("Rotate")
        .description(".")
        .defaultValue(false)
        .build()
    );
    private final Setting<Double> rotationHeight = sgRotate.add(new DoubleSetting.Builder()
        .name("Rotation Height")
        .description(".")
        .defaultValue(0.3)
        .range(0, 3)
        .sliderMax(3)
        .build()
    );

    //  Raytrace Page

    private final Setting<Boolean> placeRangeFromEyes = sgRaytrace.add(new BoolSetting.Builder()
        .name("Place Range From Eyes")
        .description("Removes crystals Instantly after spawning.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> placeRangeStartHeight = sgRaytrace.add(new DoubleSetting.Builder()
        .name("Place Range Start Height")
        .description(".")
        .defaultValue(1)
        .range(0, 3)
        .sliderMax(3)
        .visible(() -> !placeRangeFromEyes.get())
        .build()
    );
    private final Setting<Double> placeRangeHeight = sgRaytrace.add(new DoubleSetting.Builder()
        .name("Place Range End Height")
        .description(".")
        .defaultValue(1)
        .range(0, 2)
        .sliderMax(2)
        .build()
    );

    private final Setting<Boolean> breakRangeFromEyes = sgRaytrace.add(new BoolSetting.Builder()
        .name("Break Range From Eyes")
        .description("Removes crystals Instantly after spawning.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> breakRangeStartHeight = sgRaytrace.add(new DoubleSetting.Builder()
        .name("Break Range Start Height")
        .description(".")
        .defaultValue(1)
        .range(0, 3)
        .sliderMax(3)
        .visible(() -> !breakRangeFromEyes.get())
        .build()
    );
    private final Setting<Double> breakRangeHeight = sgRaytrace.add(new DoubleSetting.Builder()
        .name("Break Range End Height")
        .description(".")
        .defaultValue(1)
        .range(0, 3)
        .sliderMax(3)
        .build()
    );
    private final Setting<Boolean> raytraceFromEyes = sgRaytrace.add(new BoolSetting.Builder()
        .name("Raytrace From Eyes")
        .description("Removes crystals Instantly after spawning.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> playerRayStartHeight = sgRaytrace.add(new DoubleSetting.Builder()
        .name("Ray Start Height")
        .description(".")
        .defaultValue(1)
        .range(0, 3)
        .sliderMax(3)
        .visible(() -> !raytraceFromEyes.get())
        .build()
    );
    private final Setting<Double> rayEndHeight = sgRaytrace.add(new DoubleSetting.Builder()
        .name("Ray End Height")
        .description(".")
        .defaultValue(2)
        .range(0, 3)
        .sliderMax(3)
        .build()
    );

    //  Render Page

    private final Setting<Boolean> animation = sgRender.add(new BoolSetting.Builder()
        .name("Animation")
        .description("Removes crystals Instantly after spawning.")
        .defaultValue(false)
        .build()
    );
    private final Setting<Double> renderMoveSpeed = sgRender.add(new DoubleSetting.Builder()
        .name("Animation Move Speed")
        .description(".")
        .defaultValue(5)
        .range(0, 10)
        .sliderMax(10)
        .visible(animation::get)
        .build()
    );

    private final Setting<Double> animationSpeed = sgRender.add(new DoubleSetting.Builder()
        .name("Animation Speed")
        .description(".")
        .defaultValue(5)
        .range(0, 10)
        .sliderMax(10)
        .visible(animation::get)
        .build()
    );

    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
        .name("Color")
        .description("U blind?")
        .defaultValue(new SettingColor(255, 255, 255, 255))
        .build()
    );

    private Direction[] horizontals = new Direction[] {
        Direction.WEST,
        Direction.EAST,
        Direction.NORTH,
        Direction.SOUTH
    };

    public enum SwingMode {
        None,
        Full,
        Client,
        Packet
    }

    public enum ListenerMode {
        TickPre,
        TickPost,
        Tick,
        Render,
        MotionUpdate
    }
    private int lowest;

    protected BlockPos placePos;
    protected BlockPos lastPos;
    protected boolean blocked;
    private int lastId;
    private boolean lastPaused;
    private double renderAnim;
    private Vec3d renderPos;
    private double height;
    private List<PlayerEntity> extPos = new ArrayList<>();
    private double activeTime;
    private List<Runnable> queue = new ArrayList<>();
    private List<Double> timeQueue = new ArrayList<>();
    public AutoCrystal() {
        super(Addon.ANARCHY, "Auto Crystal", "Breaks crystals automatically.");
    }

    // Listeners

    @Override
    public void onActivate() {
        super.onActivate();
        queue = new ArrayList<>();
        timeQueue = new ArrayList<>();
        activeTime = 0;
        renderAnim = 0;
        lowest = Integer.MIN_VALUE;
        resetVar();
    }

    @EventHandler(priority = EventPriority.HIGHEST + 1)
    private void onTickPre(TickEvent.Pre event) {
        if (rotate.get() && placePos != null) {
            double yaw = Rotations.getYaw(new Vec3d(placePos.getX() + 0.5, placePos.getY() + rotationHeight.get(), placePos.getZ() + 0.5));
            double pitch = Rotations.getPitch(new Vec3d(placePos.getX() + 0.5, placePos.getY() + rotationHeight.get(), placePos.getZ() + 0.5));

            Rotations.rotate(yaw, pitch);
        }
        if (calcMode.get().equals(ListenerMode.Tick) || calcMode.get().equals(ListenerMode.TickPre)) {update();}
    }
    @EventHandler(priority = EventPriority.HIGHEST + 1)
    private void onTickPost(TickEvent.Pre event) {
        if (calcMode.get().equals(ListenerMode.Tick) || calcMode.get().equals(ListenerMode.TickPost)) {update();}
    }

    @EventHandler(priority = EventPriority.HIGHEST + 1)
    private void onMoveUpdate(PlayerMoveEvent event) {
        if (calcMode.get().equals(ListenerMode.MotionUpdate)) {update();}
    }

    @EventHandler(priority = EventPriority.HIGHEST + 1)
    private void onRender3D(Render3DEvent event) {
        activeTime += event.frameTime;
        if (!timeQueue.isEmpty()) {
            for (int i = 0; i < timeQueue.size(); i++) {
                if (timeQueue.get(i) < activeTime) {
                    timeQueue.remove(i);
                    queue.get(i).run();
                    ChatUtils.sendMsg(Text.of("ree"));
                }
            }
        }
        if (calcMode.get().equals(ListenerMode.Render)) {update();}
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onEntity(EntityAddedEvent event) {
        if (event.entity.getId() > lowest) {
            lowest = event.entity.getId();
        }
        if (mc.player != null && mc.world != null && explode.get()) {
            if (!pausedCheck()) {
                if (event.entity.getType() == EntityType.END_CRYSTAL) {
                    if (placePos != null) {
                        Box box = new Box(placePos.getX(), placePos.getY() + 1, placePos.getZ(),
                            placePos.getX() + 1, placePos.getY() + 3, placePos.getZ() + 1);
                        if ((event.entity.getBoundingBox().intersects(box) && !event.entity.getBlockPos().equals(placePos)
                            && explodeBlocking.get())) {
                            attackEntity(event.entity, true);
                            return;
                        }
                    }
                    if (canBreak(event.entity.getPos(), false)) {
                        attackEntity(event.entity, true);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onBlock(PacketEvent.Send event) {
        if (mc.player != null && mc.world != null) {
            if (event.packet instanceof PlayerActionC2SPacket) {
                PlayerActionC2SPacket packet = (PlayerActionC2SPacket) event.packet;
                Direction dir = getDirectionToEnemy(packet.getPos());
                if (packet.getAction() == PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK && dir != null) {
                    BlockPos pos = packet.getPos().offset(dir);
                    Hand swingHand = getHand(Items.END_CRYSTAL, preferMainHand.get(), true);
                    Hand handToUse = getHand(Items.END_CRYSTAL, preferMainHand.get(), false);
                    if (handToUse != null) {
                        swing(swingHand, placeSwingMode.get(), placeSwing.get(), prePlaceSwing.get(), true);
                        mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(handToUse,
                            new BlockHitResult(new Vec3d(pos.getX(), pos.getY() - 1, pos.getZ()),
                                Direction.UP, pos.down(), false), 0));
                        swing(swingHand, placeSwingMode.get(), placeSwing.get(), prePlaceSwing.get(), false);
                        if (idPredict.get()) {
                            predictAttack(highestID() + 1, new Vec3d(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5), false,
                                idDelay.get());
                        }
                        blocked = true;
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onRender(Render3DEvent event) {
        if (mc.player != null && mc.world != null) {
            if (placePos != null) {
                if (animation.get()) {
                    renderPos = smoothMove(renderPos,
                        new Vec3d(placePos.getX(), placePos.getY(), placePos.getZ()), (float) (renderMoveSpeed.get() * event.tickDelta / 10));
                } else {
                    renderPos = new Vec3d(placePos.getX(), placePos.getY(), placePos.getZ());
                }
            }
            if (animation.get()) {
                renderAnim = placePos != null ?
                    (renderAnim + animationSpeed.get() > 100 ? 100 : renderAnim + animationSpeed.get())
                    :
                    (renderAnim - animationSpeed.get() < 0 ? 0 : renderAnim - animationSpeed.get());
            }
            if (renderPos != null && (!animation.get() || renderAnim > 0)) {
                Vec3d v = new Vec3d(renderPos.getX() + 0.5, renderPos.getY() + 0.5, renderPos.getZ() + 0.5);
                double progress = renderAnim / 100 / 2;
                Box toRender = new Box(v.x - progress, v.y - progress + height, v.z - progress, v.x + progress, v.y + progress - height, v.z + progress);
                event.renderer.box(toRender, new Color(color.get().r, color.get().g, color.get().b,
                    (int) Math.floor(color.get().a / 5f)), color.get(), ShapeMode.Both, 0);
            }
        }
    }

    // Other stuff

    private void update() {
        if (mc.player != null && mc.world != null) {
            placePos = findBestPos();
            if (placePos != null) {
                if (pausedCheck() != lastPaused) {
                    lastPaused = pausedCheck();
                    resetVar();
                }
                if (!pausedCheck()) {
                    if (!placePos.equals(lastPos)) {
                        resetVar();
                    }
                    else if (crystalAtPos(placePos.up()) == null && isAround(placePos.up()) != null &&
                        alwaysExplodeBlocking.get() && explode.get()) {
                        cleanPos(placePos.up());
                    }
                    if (!blocked && place.get()) {
                        Hand swingHand = getHand(Items.END_CRYSTAL, preferMainHand.get(), true);
                        Hand handToUse = getHand(Items.END_CRYSTAL, preferMainHand.get(), false);
                        if (handToUse != null) {
                            swing(swingHand, placeSwingMode.get(), placeSwing.get(), prePlaceSwing.get(), true);
                            mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(handToUse,
                                new BlockHitResult(new Vec3d(placePos.getX(), placePos.getY(), placePos.getZ()), Direction.UP, placePos, false), 0));
                            swing(swingHand, placeSwingMode.get(), placeSwing.get(), prePlaceSwing.get(), false);
                            if (idPredict.get()) {
                                predictAttack(highestID() + 1, new Vec3d(placePos.getX() + 0.5, placePos.getY() + 1, placePos.getZ() + 0.5), false,
                                    idDelay.get());
                            }
                            blocked = true;
                        }
                    }
                }
            } else {
                lastPos = null;
            }
        }
    }

    private BlockPos findBestPos() {
        BlockPos position = null;
        if (mc.player != null && mc.world != null) {
            double highestDMG = 0;
            double highestDist = 0;
            int calcRange = (int) Math.ceil(placeRange.get());
            for (int y = calcRange; y >= -calcRange; y--) {
                for (int x = -calcRange; x <= calcRange; x++) {
                    for (int z = -calcRange; z <= calcRange; z++) {
                        BlockPos pos = new BlockPos(x + mc.player.getBlockPos().getX(),
                            y + mc.player.getBlockPos().getY(), z + mc.player.getBlockPos().getZ());
                        if (canBePlaced(pos)) {
                            double dmg[] = highestDmg(pos);
                            double self = getSelfDamage(new Vec3d(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5), pos);
                            double dist = OLEPOSSUtils.distance(new Vec3d(x + mc.player.getBlockPos().getX() + 0.5,
                                y + mc.player.getBlockPos().getY(), z + mc.player.getBlockPos().getZ() + 0.5),
                                playerRangePos(true));
                            if (placeDamageCheck(dmg[0], self, dmg[1], highestDMG, dist, highestDist)) {
                                highestDMG = dmg[0];
                                highestDist = dist;
                                position = pos;
                            }
                        }
                    }
                }
            }
        }
        return position;
    }

    private boolean placeDamageCheck(double dmg, double self, double health, double highest, double distance, double highestDist) {
        if (dmg < highest) {return false;}
        if (dmg == highest && distance > highestDist) {return false;}

        //  Force pop check
        double playerHP = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        boolean[] valid = new boolean[] {forcePop.get() * dmg > health, playerHP - (antiPop.get() * dmg) > minHealthLeft.get()};
        if (valid[0] && (valid[1] || suicidal.get())) {return true;}

        if (dmg < minPlaceDamage.get()) {return false;}
        return self / dmg <= maxSelfPlace.get();
    }

    private boolean breakDamageCheck(double dmg, double self, double health) {
        if (ignoreBreak.get()) {return true;}

        //  Force pop check
        double playerHP = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        boolean[] valid = new boolean[] {forcePop.get() * dmg > health, playerHP - (antiPop.get() * dmg) > minHealthLeft.get()};
        if (valid[0] && (valid[1] || suicidal.get())) {return true;}

        return self / dmg <= maxSelfBreak.get() && dmg < minExplodeDamage.get();
    }

    protected double[] highestDmg(BlockPos pos) {
        double highest = 0;
        double highestHP = 0;
        if (mc.player != null && mc.world != null) {
            for (PlayerEntity enemy : mc.world.getPlayers()) {
                if (enemy != mc.player && !Friends.get().isFriend(enemy) && (!deadCheck.get() || enemy.getHealth() > 0)) {
                    double dmg = DamageUtils.crystalDamage(enemy, new Vec3d(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5),
                        false, pos, false);
                    if (dmg > highest) {
                        highest = dmg;
                        highestHP = enemy.getHealth() + enemy.getAbsorptionAmount();
                    }
                }
            }
        }
        return new double[] {highest, highestHP};
    }

    protected boolean canBePlaced(BlockPos pos) {
        if (mc.player != null && mc.world != null) {
            if (mc.world.getBlockState(pos).getBlock() != Blocks.OBSIDIAN &&
                mc.world.getBlockState(pos).getBlock() != Blocks.BEDROCK) {
                return false;
            }

            if (!mc.world.getBlockState(pos.offset(Direction.UP)).getBlock().equals(Blocks.AIR)) {
                return false;
            }
            if (!placeRangeCheck(pos)) {
                return false;
            }
            Box box = new Box(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX() + 1, pos.getY() + 3, pos.getZ() + 1);
            return !EntityUtils.intersectsWithEntity(box, entity -> !entity.isSpectator() && !(
                entity.getBlockPos().equals(pos.up()) && entity.getType() == EntityType.END_CRYSTAL
            ));
        }
        return false;
    }

    private Vec3d playerRangePos(boolean place) {
        if (place) {
            return new Vec3d(mc.player.getX(),
                placeRangeFromEyes.get() ? mc.player.getEyePos().y : mc.player.getY() + placeRangeStartHeight.get(), mc.player.getZ());
        } else {
            return new Vec3d(mc.player.getX(),
                breakRangeFromEyes.get() ? mc.player.getEyePos().y : mc.player.getY() + breakRangeStartHeight.get(), mc.player.getZ());
        }
    }

    private boolean placeRangeCheck(BlockPos pos) {
        return (OLEPOSSUtils.distance(playerRangePos(true),
            new Vec3d(pos.getX() + 0.5, pos.getY() + placeRangeHeight.get(), pos.getZ() + 0.5)) <= placeRange.get()) &&
            (!superSmartRangeChecks.get() || breakRangeCheck(new Vec3d(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5)));
    }

    private boolean breakRangeCheck(Vec3d pos) {
        return OLEPOSSUtils.distance(playerRangePos(false),
            new Vec3d(pos.getX(), pos.getY() + breakRangeHeight.get(), pos.getZ())) <= getBreakRange(pos);
    }

    private Hand getHand(Item item, boolean preferMain, boolean swing) {
        if (!mc.player.isHolding(item) && !swing) {
            return null;
        }
        if (allowOffhand.get() && mc.player.getOffHandStack().getItem() == item) {
            if (preferMain && mc.player.getMainHandStack().getItem() == item) {
                return Hand.MAIN_HAND;
            } else {
                return Hand.OFF_HAND;
            }
        } else if (mc.player.getMainHandStack().getItem() == item) {
            return Hand.MAIN_HAND;
        }
        return swing ? Hand.MAIN_HAND : null;
    }

    private double getBreakRange(Vec3d pos) {
        Vec3d vec1 = new Vec3d(mc.player.getX(), raytraceFromEyes.get() ? mc.player.getEyePos().getY() :
            mc.player.getY() + playerRayStartHeight.get(), mc.player.getZ());
        Vec3d vec2 = new Vec3d(pos.getX(), pos.getY() + rayEndHeight.get(), pos.getZ());
        if (mc.world.raycast(new RaycastContext(vec1, vec2,
            RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player)).getType() != HitResult.Type.BLOCK) {
            return explodeRange.get();
        } else {
            return explodeWalls.get();
        }
    }

    private boolean canBreak(Vec3d pos, boolean clean) {
        boolean[] i = new boolean[] {clean && damageIgnBlocking.get() && explodeBlocking.get(),
            clean && rangeIgnBlocking.get() && explodeBlocking.get()};
        if (!explode.get()) {return false;}
        double self = getSelfDamage(pos ,new BlockPos(Math.floor(pos.x), Math.floor(pos.y) - 1, Math.floor(pos.z)));
        if (onlyExplodeWhenHolding.get() && getHand(Items.END_CRYSTAL, preferMainHand.get(), false) == null) {return false;}
        double[] dmg = highestDmg(new BlockPos(Math.floor(pos.x), Math.floor(pos.y) - 1, Math.floor(pos.z)));
        if (!breakDamageCheck(dmg[0], self, dmg[1]) && !i[0]) {return false;}
        return breakRangeCheck(new Vec3d(pos.x, pos.y, pos.z)) || i[1];
    }

    private int highestID() {
        int highest = lastId;
        if (mc.world != null) {
            for (Entity entity : mc.world.getEntities()) {
                if (entity.getId() > highest) {
                    highest = entity.getId();
                }
            }
        }
        lastId = highest + 1;
        return highest;
    }

    private void cleanPos(BlockPos pos) {
        if (mc.player != null && mc.world != null) {
            Box box = new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1);
            if (EntityUtils.intersectsWithEntity(box, entity -> entity.getType() == EntityType.END_CRYSTAL)) {
                for (Entity en : mc.world.getEntities()) {
                    if (en.getBoundingBox().intersects(box) && en instanceof EndCrystalEntity) {
                        if (canBreak(en.getPos(), true)) {
                            attackEntity(en, true);
                            break;
                        }
                    }
                }
            }
        }
    }

    private EndCrystalEntity isAround(BlockPos pos) {
        Box box = new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1);
        for (Entity en : mc.world.getEntities()) {
            if (en.getBoundingBox().intersects(box) && en instanceof EndCrystalEntity && !en.getBlockPos().equals(pos)) {
                return (EndCrystalEntity) en;
            }
        }
        return null;
    }

    private EndCrystalEntity crystalAtPos(BlockPos pos) {
        if (mc.world != null) {
            for (Entity en : mc.world.getEntities()) {
                if (en instanceof EndCrystalEntity && en.getBlockPos().equals(pos)) {
                    return (EndCrystalEntity) en;
                }
            }
        }
       return null;
    }

    private void resetVar() {
        lastId = Integer.MIN_VALUE;
        if (placePos != null) {
            blocked = crystalAtPos(placePos.up()) != null;
        } else {
            blocked = false;
        }
        lastPos = placePos;
        if (blocked && explode.get()) {
            cleanPos(placePos.up());
        }
    }

    private void swing(Hand hand, SwingMode mode, boolean mainSetting, boolean timingSetting, boolean pre) {
        if (mainSetting && mc.player != null) {
            if (timingSetting == pre) {
                if (mode == SwingMode.Full || mode == SwingMode.Packet) {
                    mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(hand));
                }
                if (mode == SwingMode.Full || mode == SwingMode.Client) {
                    mc.player.swingHand(hand);
                }
            }
        }
    }
    private void predictAttack(int id, Vec3d pos, boolean checkSD, double delay) {
        queue.add(() -> attackID(id, pos, checkSD));
        timeQueue.add(activeTime + delay);
    }

    private void attackID(int id, Vec3d pos, boolean checkSD) {
        EndCrystalEntity en = new EndCrystalEntity(mc.world, pos.x, pos.y, pos.z);
        en.setId(id);
        attackEntity(en, checkSD);
    }

    private void attackEntity(Entity en, boolean checkSD) {
        if (mc.player != null) {
            Hand handToUse = getHand(Items.END_CRYSTAL, preferMainHand.get(), true);
            if (handToUse != null) {
                blocked = false;
                swing(handToUse, explodeSwingMode.get(), explodeSwing.get(), preExplodeSwing.get(), true);
                mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(en, mc.player.isSneaking()));
                swing(handToUse, explodeSwingMode.get(), explodeSwing.get(), preExplodeSwing.get(), false);
                if (setDead.get() && checkSD) {
                    en.setRemoved(Entity.RemovalReason.KILLED);
                }
            }
        }
    }

    private boolean pausedCheck() {
        if (mc.player != null) {
            return eatPause.get() && (mc.player.isUsingItem() && mc.player.isHolding(Items.ENCHANTED_GOLDEN_APPLE));
        }
        return true;
    }

    public Vec3d smoothMove(Vec3d current, Vec3d target, float speed) {
        if (current == null) {
            return target;
        }
        double absX = Math.abs(current.x - target.x);
        double absY = Math.abs(current.y - target.y);
        double absZ = Math.abs(current.z - target.z);
        height = Math.sqrt(absX * absX + absZ * absZ) * speed;

        return new Vec3d(
            current.x > target.x ?
                (absX <= speed * absX ? target.x : current.x - speed * absX) :
                current.x != target.x ?
                    (absX <= speed * absX ? target.x : current.x + speed * absX) :
                    target.x
            ,
            current.y > target.y ?
                (absY <= speed * absY ? target.y : current.y - speed * absY) :
                current.y != target.y ?
                    (absY <= speed * absY ? target.y : current.y + speed * absY) :
                    target.y
            ,
            current.z > target.z ?
                (absZ <= speed * absZ ? target.z : current.z - speed * absZ) :
                current.z != target.z ?
                    (absZ <= speed * absZ ? target.z : current.z + speed * absZ) :
                    target.z);
    }

    public Direction getDirectionToEnemy(BlockPos block) {
        for (PlayerEntity pl : mc.world.getPlayers()) {
            Direction dir = nextTo(pl, block);
            if (dir != null) {
                return dir;
            }
        }
        return null;
    }

    public Direction nextTo(PlayerEntity pl, BlockPos block) {
        for (Direction dir : horizontals) {
            if (pl.getBlockPos().offset(dir) == block) {
                return dir;
            }
        }
        return null;
    }

    private double getSelfDamage(Vec3d vec, BlockPos pos) {
        return DamageUtils.crystalDamage(mc.player, vec,
            false, pos.down(), false);
    }
}
