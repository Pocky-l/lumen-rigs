package com.pockyl.lumen_rigs.fixture;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Aiming math shared by server and client. */
public final class Aim {
    private Aim() {
    }

    /** Unit vector for a pan (Minecraft yaw: 0 = south, 90 = west) and tilt (positive up), in degrees. */
    public static Vec3 direction(float pan, float tilt) {
        // Exact trigonometry: Minecraft's table-based sin/cos are too coarse for a beam 64 blocks long.
        double yaw = Math.toRadians(pan);
        double pitch = Math.toRadians(tilt);
        double horizontal = Math.cos(pitch);
        return new Vec3(-Math.sin(yaw) * horizontal, Math.sin(pitch), Math.cos(yaw) * horizontal);
    }

    public static float pan(Vec3 direction) {
        return Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(-direction.x, direction.z)));
    }

    public static float tilt(Vec3 direction) {
        double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        return (float) Math.toDegrees(Math.atan2(direction.y, horizontal));
    }

    /** Rotation from the fixture's own frame (standing on the floor, up = +Y) to its mount face. */
    public static Quaternionf mountRotation(Direction facing) {
        return switch (facing) {
            case UP -> new Quaternionf();
            case DOWN -> new Quaternionf().rotationX(Mth.PI);
            case NORTH -> new Quaternionf().rotationX(-Mth.HALF_PI);
            case SOUTH -> new Quaternionf().rotationX(Mth.HALF_PI);
            case EAST -> new Quaternionf().rotationZ(-Mth.HALF_PI);
            case WEST -> new Quaternionf().rotationZ(Mth.HALF_PI);
        };
    }

    /** A point given in the fixture's frame (blocks, from the block corner) in world coordinates. */
    public static Vec3 toWorld(Direction facing, double x, double y, double z, double blockX, double blockY, double blockZ) {
        Vector3f local = new Vector3f((float) (x - 0.5), (float) (y - 0.5), (float) (z - 0.5));
        mountRotation(facing).transform(local);
        return new Vec3(blockX + 0.5 + local.x, blockY + 0.5 + local.y, blockZ + 0.5 + local.z);
    }

    /** Turns {@code current} towards {@code target} by at most {@code maxDegrees}; both are unit vectors. */
    public static Vec3 turnTowards(Vec3 current, Vec3 target, float maxDegrees) {
        double angle = Math.acos(Mth.clamp(current.dot(target), -1, 1));
        double max = Math.toRadians(maxDegrees);
        if (angle <= max || angle < 1.0E-4) {
            return target;
        }
        Vec3 axis = current.cross(target);
        if (axis.lengthSqr() < 1.0E-8) {
            // Opposite directions: turn around any perpendicular axis.
            axis = Math.abs(current.y) < 0.9 ? current.cross(new Vec3(0, 1, 0)) : current.cross(new Vec3(1, 0, 0));
        }
        axis = axis.normalize();
        // Rodrigues rotation by the allowed angle.
        double cos = Math.cos(max);
        double sin = Math.sin(max);
        return current.scale(cos).add(axis.cross(current).scale(sin)).add(axis.scale(axis.dot(current) * (1 - cos))).normalize();
    }
}
