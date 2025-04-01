package org.tensortapestry.zspace;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.tensortapestry.zspace.indexing.BufferOwnership;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

@ThreadSafe
@Immutable
@Value
@EqualsAndHashCode(cacheStrategy = EqualsAndHashCode.CacheStrategy.LAZY)
public class ZPointSet implements ZPointCollection<ZPointSet> {
    int ndim;
    Set<ZPoint> points;

    /**
     * Create a new ZPointSet from a set of ZPoints.
     *
     * @param points the points.
     */
    public ZPointSet(int ndim, @Nonnull Set<ZPoint> points) {
        this(true, ndim, BufferOwnership.CLONED, points);
    }

    private ZPointSet(
            boolean testDim,
            int ndim,
            @Nonnull BufferOwnership pointsOwnership,
            @Nonnull Set<ZPoint> points

    ) {
        this.ndim = ndim;
        if (pointsOwnership == BufferOwnership.REUSED) {
            this.points = points;
        } else {
            this.points = Set.copyOf(points);
        }
        if (testDim) {
            for (ZPoint p : this.points) {
                p.assertNDim(this.ndim);
            }
        }
    }

    @Override
    public int getNDim() {
        return points.stream().findFirst().map(ZPoint::getNDim).orElse(0);
    }

    @Override
    public int getSize() {
        return points.size();
    }

    @Override
    public boolean contains(@Nonnull ZTensorWrapper p) {
        return points.contains(p);
    }

    @Override
    public @Nonnull Iterator<ZPoint> iterator() {
        return points.iterator();
    }

    @Override
    public @Nonnull ZPointSet permute(@Nonnull int... permutation) {
        return new ZPointSet(
                false,
                ndim,
                BufferOwnership.REUSED,
                points.stream().map(p -> p.permute(permutation)).collect(Collectors.toSet()));
    }

}
