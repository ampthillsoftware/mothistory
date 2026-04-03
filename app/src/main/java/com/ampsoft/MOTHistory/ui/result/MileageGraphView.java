package com.ampsoft.MOTHistory.ui.result;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.ampsoft.MOTHistory.R;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MileageGraphView extends View {

    private static final int MAX_X_AXIS_LABELS = 4;

    private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint areaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointInnerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint chipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path linePath = new Path();
    private final Path areaPath = new Path();
    private final RectF chipRect = new RectF();

    private final List<MotHistoryInsights.MileagePoint> points = new ArrayList<>();

    public MileageGraphView(Context context) {
        this(context, null);
    }

    public MileageGraphView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        int primary = ContextCompat.getColor(context, R.color.mot_blue);
        int neutral = ContextCompat.getColor(context, R.color.status_neutral);
        int surface = ContextCompat.getColor(context, R.color.card_surface);

        axisPaint.setColor(adjustAlpha(neutral, 0.42f));
        axisPaint.setStrokeWidth(dp(1f));

        gridPaint.setColor(adjustAlpha(neutral, 0.16f));
        gridPaint.setStrokeWidth(dp(1f));

        areaPaint.setColor(adjustAlpha(primary, 0.14f));
        areaPaint.setStyle(Paint.Style.FILL);

        linePaint.setColor(primary);
        linePaint.setStrokeWidth(dp(3f));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setStrokeCap(Paint.Cap.ROUND);

        pointPaint.setColor(primary);
        pointPaint.setStyle(Paint.Style.FILL);

        pointInnerPaint.setColor(surface);
        pointInnerPaint.setStyle(Paint.Style.FILL);

        labelPaint.setColor(adjustAlpha(neutral, 0.92f));
        labelPaint.setTextSize(sp(12f));

        valuePaint.setColor(adjustAlpha(neutral, 0.82f));
        valuePaint.setTextSize(sp(11f));

        chipPaint.setColor(adjustAlpha(primary, 0.18f));
        chipPaint.setStyle(Paint.Style.FILL);
    }

    public void setPoints(List<MotHistoryInsights.MileagePoint> mileagePoints) {
        points.clear();
        if (mileagePoints != null) {
            points.addAll(mileagePoints);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float left = dp(48f);
        float right = getWidth() - dp(18f);
        float top = dp(24f);
        float bottom = getHeight() - dp(52f);
        float width = right - left;
        float height = bottom - top;

        if (width <= 0 || height <= 0) {
            return;
        }

        drawBase(canvas, left, top, right, bottom);
        if (points.isEmpty()) {
            return;
        }

        long min = points.get(0).getMileageValue();
        long max = points.get(0).getMileageValue();
        for (MotHistoryInsights.MileagePoint point : points) {
            min = Math.min(min, point.getMileageValue());
            max = Math.max(max, point.getMileageValue());
        }

        long range = Math.max(1L, max - min);
        float xStep = points.size() == 1 ? 0f : width / (points.size() - 1);
        float topInset = Math.max(dp(12f), height * 0.08f);
        float drawableTop = top + topInset;
        float drawableHeight = Math.max(dp(52f), height - (topInset * 1.35f));

        linePath.reset();
        areaPath.reset();

        for (int i = 0; i < points.size(); i++) {
            MotHistoryInsights.MileagePoint point = points.get(i);
            float x = left + (xStep * i);
            float y = calculateY(point.getMileageValue(), min, range, drawableTop, drawableHeight, bottom);

            if (i == 0) {
                linePath.moveTo(x, y);
                areaPath.moveTo(x, bottom);
                areaPath.lineTo(x, y);
            } else {
                linePath.lineTo(x, y);
                areaPath.lineTo(x, y);
            }
        }

        float finalX = left + (xStep * Math.max(0, points.size() - 1));
        areaPath.lineTo(finalX, bottom);
        areaPath.close();

        canvas.drawPath(areaPath, areaPaint);
        canvas.drawPath(linePath, linePaint);

        drawValueGuides(canvas, top, bottom, min, max);

        Set<Integer> xAxisLabelIndexes = getXAxisLabelIndexes(points.size());
        for (int i = 0; i < points.size(); i++) {
            MotHistoryInsights.MileagePoint point = points.get(i);
            float x = left + (xStep * i);
            float y = calculateY(point.getMileageValue(), min, range, drawableTop, drawableHeight, bottom);

            float pointRadius = (i == 0 || i == points.size() - 1) ? dp(5f) : dp(3.5f);
            canvas.drawCircle(x, y, pointRadius, pointPaint);
            canvas.drawCircle(x, y, Math.max(dp(1.4f), pointRadius - dp(2.1f)), pointInnerPaint);

            if (xAxisLabelIndexes.contains(i)) {
                String yearLabel = toYearLabel(point.getMotTest().getCompletedDate());
                float labelWidth = labelPaint.measureText(yearLabel);
                float labelX = clamp(x - (labelWidth / 2f), left, right - labelWidth);
                canvas.drawText(yearLabel, labelX, bottom + dp(24f), labelPaint);
            }

            if (i == 0 || i == points.size() - 1) {
                drawValueChip(canvas, x, y - dp(18f), compactMileage(point.getMileageValue()), left, right);
            }
        }
    }

    private void drawBase(Canvas canvas, float left, float top, float right, float bottom) {
        float midY = top + ((bottom - top) / 2f);
        canvas.drawLine(left, bottom, right, bottom, axisPaint);
        canvas.drawLine(left, top, left, bottom, axisPaint);
        canvas.drawLine(left, top, right, top, gridPaint);
        canvas.drawLine(left, midY, right, midY, gridPaint);
    }

    private void drawValueGuides(Canvas canvas, float top, float bottom, long min, long max) {
        canvas.drawText(compactMileage(max), dp(6f), top + dp(4f), valuePaint);
        if (max != min) {
            float midY = top + ((bottom - top) / 2f);
            canvas.drawText(compactMileage((max + min) / 2L), dp(6f), midY + dp(4f), valuePaint);
        }
        canvas.drawText(compactMileage(min), dp(6f), bottom + dp(4f), valuePaint);
    }

    private void drawValueChip(
            Canvas canvas,
            float centerX,
            float baselineY,
            String label,
            float chartLeft,
            float chartRight
    ) {
        float horizontalPadding = dp(8f);
        float verticalPadding = dp(5f);
        float textWidth = valuePaint.measureText(label);
        Paint.FontMetrics metrics = valuePaint.getFontMetrics();

        float left = clamp(
                centerX - (textWidth / 2f) - horizontalPadding,
                chartLeft,
                chartRight - textWidth - (horizontalPadding * 2f)
        );
        float top = baselineY + metrics.ascent - verticalPadding;
        float right = left + textWidth + (horizontalPadding * 2f);
        float bottom = baselineY + metrics.descent + verticalPadding;

        chipRect.set(left, top, right, bottom);
        canvas.drawRoundRect(chipRect, dp(10f), dp(10f), chipPaint);
        canvas.drawText(label, left + horizontalPadding, baselineY, valuePaint);
    }

    private float calculateY(
            long mileageValue,
            long min,
            long range,
            float drawableTop,
            float drawableHeight,
            float bottom
    ) {
        float y = bottom - (((float) (mileageValue - min) / range) * drawableHeight);
        return Math.max(drawableTop, y);
    }

    private float dp(float value) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        );
    }

    private float sp(float value) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                value,
                getResources().getDisplayMetrics()
        );
    }

    private int adjustAlpha(int color, float factor) {
        return Color.argb(
                Math.round(Color.alpha(color) * factor),
                Color.red(color),
                Color.green(color),
                Color.blue(color)
        );
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private Set<Integer> getXAxisLabelIndexes(int pointCount) {
        Set<Integer> indexes = new LinkedHashSet<>();
        if (pointCount <= 0) {
            return indexes;
        }
        if (pointCount <= MAX_X_AXIS_LABELS) {
            for (int i = 0; i < pointCount; i++) {
                indexes.add(i);
            }
            return indexes;
        }

        indexes.add(0);
        for (int slot = 1; slot < MAX_X_AXIS_LABELS - 1; slot++) {
            int index = Math.round(((pointCount - 1f) * slot) / (MAX_X_AXIS_LABELS - 1f));
            indexes.add(index);
        }
        indexes.add(pointCount - 1);
        return indexes;
    }

    private String toYearLabel(String isoDate) {
        if (isoDate == null || isoDate.trim().isEmpty()) {
            return "";
        }
        try {
            return String.valueOf(OffsetDateTime.parse(isoDate.trim()).getYear());
        } catch (DateTimeParseException e) {
            return isoDate.length() >= 4 ? isoDate.substring(0, 4) : isoDate;
        }
    }

    private String compactMileage(long value) {
        if (value >= 1000) {
            return String.format(Locale.UK, "%.0fk", value / 1000f);
        }
        return String.valueOf(value);
    }
}
