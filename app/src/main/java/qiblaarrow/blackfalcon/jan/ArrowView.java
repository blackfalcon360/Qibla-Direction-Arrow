package qiblaarrow.blackfalcon.jan;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.view.View;

/** Black screen, one long arrow that points to the Qibla, with the details below it. */
public class ArrowView extends View {

    private static final int GOLD = 0xFFC9A227;
    private static final int GREEN = 0xFF3DDC84;
    private static final int GRAY = 0xFF9AA0A6;

    private final float dp;
    private final Paint arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint degreesPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint infoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint headingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint messagePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint creditPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path arrow = new Path();

    private boolean hasLocation;
    private double qiblaBearing;
    private float relative;
    private float heading;
    private String message = "";

    public ArrowView(Context context) {
        super(context);
        dp = getResources().getDisplayMetrics().density;
        float sp = getResources().getDisplayMetrics().scaledDensity;

        arrowPaint.setStyle(Paint.Style.FILL);

        labelPaint.setColor(GRAY);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTextSize(20 * sp);

        degreesPaint.setColor(Color.WHITE);
        degreesPaint.setTextAlign(Paint.Align.CENTER);
        degreesPaint.setTypeface(Typeface.DEFAULT_BOLD);
        degreesPaint.setTextSize(60 * sp);

        infoPaint.setTextAlign(Paint.Align.CENTER);
        infoPaint.setTypeface(Typeface.DEFAULT_BOLD);
        infoPaint.setTextSize(19 * sp);

        headingPaint.setColor(GRAY);
        headingPaint.setTextAlign(Paint.Align.CENTER);
        headingPaint.setTextSize(14 * sp);

        messagePaint.setColor(Color.WHITE);
        messagePaint.setTextAlign(Paint.Align.CENTER);
        messagePaint.setTextSize(17 * sp);

        creditPaint.setColor(GOLD);
        creditPaint.setTextAlign(Paint.Align.RIGHT);
        creditPaint.setTypeface(Typeface.DEFAULT_BOLD);
        creditPaint.setTextSize(15 * sp);
    }

    /** rel = angle you must turn to face the Qibla (positive = right). */
    public void setState(boolean hasLocation, double qiblaBearing, float rel, float heading, String message) {
        this.hasLocation = hasLocation;
        this.qiblaBearing = qiblaBearing;
        this.relative = rel;
        this.heading = heading;
        this.message = message == null ? "" : message;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        canvas.drawColor(Color.BLACK);

        // mandatory credit, top-right corner
        canvas.drawText("By: Black Falcon", w - 20 * dp, 28 * dp + creditPaint.getTextSize(), creditPaint);

        float cx = w / 2f;
        float cy = h * 0.40f;
        boolean aligned = hasLocation && Math.abs(relative) <= 3f;

        if (hasLocation) {
            // long arrow: tip at the top, rotated about its centre
            float length = Math.min(w * 0.94f, h * 0.55f);
            float half = length / 2f;
            float headLen = length * 0.28f;
            float headHalfW = length * 0.16f;
            float shaftHalfW = length * 0.045f;

            arrow.reset();
            arrow.moveTo(0f, -half);
            arrow.lineTo(headHalfW, -half + headLen);
            arrow.lineTo(shaftHalfW, -half + headLen);
            arrow.lineTo(shaftHalfW, half);
            arrow.lineTo(-shaftHalfW, half);
            arrow.lineTo(-shaftHalfW, -half + headLen);
            arrow.lineTo(-headHalfW, -half + headLen);
            arrow.close();

            arrowPaint.setColor(aligned ? GREEN : Color.WHITE);
            canvas.save();
            canvas.translate(cx, cy);
            canvas.rotate(relative);
            canvas.drawPath(arrow, arrowPaint);
            canvas.restore();
        } else {
            float y = cy;
            for (String line : message.split("\n")) {
                canvas.drawText(line, cx, y, messagePaint);
                y += messagePaint.getTextSize() * 1.5f;
            }
        }

        // details below the arrow
        canvas.drawText("Qibla Direction", cx, h * 0.755f, labelPaint);

        String deg = hasLocation ? (Math.round(qiblaBearing) % 360) + "\u00B0" : "--\u00B0";
        canvas.drawText(deg, cx, h * 0.845f, degreesPaint);

        if (hasLocation) {
            int a = Math.round(Math.abs(relative));
            String info = aligned ? "Facing the Qibla" : "Turn " + a + "\u00B0 " + (relative > 0 ? "right" : "left");
            infoPaint.setColor(aligned ? GREEN : GOLD);
            canvas.drawText(info, cx, h * 0.895f, infoPaint);
            canvas.drawText("Your heading " + (Math.round(heading) % 360) + "\u00B0", cx, h * 0.935f, headingPaint);
        }
    }
}
