/*
 * This file is part of PlayRecorder.
 *
 * PlayRecorder is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PlayRecorder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.halman.playrecorder;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.res.ResourcesCompat;

import java.util.HashMap;
import java.util.Map;

public class ScoreView extends View {
    private Drawable gclef = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_g_clef, null);
    private Drawable fclef = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_f_clef, null);
    private Drawable note = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_whole_note, null);
    private Drawable arrowUp = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_arrow_up, null);
    private Drawable arrowDown = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_arrow_down, null);
    private Drawable sharp = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_sharp, null);
    private Drawable flat = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_flat, null);
    private Drawable natural = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_natural, null);
    private ShapeDrawable line = new ShapeDrawable(new RectShape());
    private View grip = null;

    enum Buttons {
        UP,
        DOWN,
        HALFUP,
        HALFDOWN,
        SCALEUP,
        SCALEDOWN,
        SETNOTE,
        CLEF,
    }

    private double scalefactor = 1.0;
    private int score_offset_x = 0;
    private int score_offset_y = 180;
    private int score_height = 450;
    private int score_width = 450;
    private int note_position = 350;
    private int score_center_x = 0;
    private int score_center_y = 0;

    Map<Buttons, Rect> buttonPositions = new HashMap<Buttons, Rect>() {{
        put(Buttons.UP,        new Rect(note_position - 80, 10, note_position - 10, 80));
        put(Buttons.DOWN,      new Rect(note_position - 80, score_height - 80, note_position - 10, score_height - 10));
        put(Buttons.HALFUP,    new Rect(note_position + 10, 10, note_position + 90, 80));
        put(Buttons.HALFDOWN,  new Rect(note_position + 10, score_height - 80, note_position + 90, score_height - 10));
        put(Buttons.SCALEUP,   new Rect(100, 10, 170, 80));
        put(Buttons.SCALEDOWN, new Rect(100, score_height - 80, 170, score_height - 10));
        put(Buttons.SETNOTE,   new Rect(note_position - 35,80, note_position + 35, score_height - 80));
        put(Buttons.CLEF,      new Rect(score_offset_x + 15,score_offset_y - 15, score_offset_x + 65, score_offset_y + 5 * 20 + 15));
    }};

    MainActivity activity = null;

    private Point touchdown = new Point (0, 0);

    public ScoreView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(context);
    }

    public ScoreView(Context context)
    {
        super(context);
        init(context);
    }

    void onClick(PointF point)
    {
        int x = unscale(Math.round(point.x) - score_center_x);
        int y = unscale(Math.round(point.y) - score_center_y);

        for (Map.Entry<Buttons, Rect> item: buttonPositions.entrySet()) {
            Rect r = item.getValue();
            if (r.contains (x, y)) {
                switch (item.getKey()) {
                    case UP:
                        noteUp();
                        return;
                    case DOWN:
                        noteDown();
                        return;
                    case HALFUP:
                        noteUpHalf();
                        return;
                    case HALFDOWN:
                        noteDownHalf();
                        return;
                    case SCALEUP:
                        scaleUp();
                        return;
                    case SCALEDOWN:
                        scaleDown();
                        return;
                    case SETNOTE:
                        setNote(y);
                        return;
                    case CLEF:
                        activity.onClef();
                        return;
                }
            }
        }
    }

    int scale(int dim)
    {
        return (int)Math.round(dim * scalefactor);
    }

    int unscale(int dim)
    {
        return (int)Math.round(dim / scalefactor);
    }

    private void init(Context context) {
        activity = (MainActivity) context;
    }

    int getItemCenterX(Drawable drawable) {
        return drawable.getIntrinsicWidth() / 2;
    }

    int getItemCenterY(Drawable drawable) {
        if (drawable == gclef) {
            return drawable.getIntrinsicHeight() * 75 / 100;
        }
        if (drawable == fclef) {
            return drawable.getIntrinsicHeight() * 25 / 100;
        }
        if (drawable == flat) {
            return drawable.getIntrinsicHeight() * 72 / 100;
        }
        return drawable.getIntrinsicHeight() / 2;
    }

    void putDrawable(int x, int y, Drawable drawable, Canvas canvas, double zoom)
    {
        int offsetx = getItemCenterX(drawable);
        int offsety = getItemCenterY(drawable);
        drawable.setBounds(
                scale(x - (int)(offsetx * zoom)) + score_center_x,
                scale(y - (int)(offsety * zoom)) + score_center_y,
                scale(x + (int)((-offsetx + drawable.getIntrinsicWidth()) * zoom)) + score_center_x,
                scale(y + (int)((-offsety + drawable.getIntrinsicHeight()) * zoom)) + score_center_y);
        drawable.draw(canvas);
    }

    void putDrawable(int x, int y, Drawable drawable, Canvas canvas) {
        putDrawable(x, y, drawable, canvas, 1.0);
    }

    void drawLine(int x, int y, int width, int height, Canvas canvas)
    {
        int width_scaled = scale(width);
        if (width_scaled < 2) {
            width_scaled = 2;
        }
        int height_scaled = scale(height);
        if (height_scaled < 2) {
            height_scaled = 2;
        }
        line.getPaint().setColor(0xff000000);
        line.setBounds(
                scale(x) + score_center_x,
                scale(y) + score_center_y,
                scale(x) + width_scaled + score_center_x,
                scale(y) + height_scaled + score_center_y);
        line.draw(canvas);
    }

    void drawScoreLines(Canvas canvas)
    {
        for (int a = 0; a < 5; a++) {
            drawLine(score_offset_x, score_offset_y + a*20 + 1, score_width, 2, canvas);
        }
   }

    void drawSignature(Canvas canvas) {
        int [] accidentalsPositions = new int []{3,7,4,8,5,9,6,0,10,7,11,8,5,9,6};

        if (activity == null || activity.app == null) {
            return;
        }

        int signature = activity.app.scale.signature();
        int offset = 0;
        if (activity.app.clef() == Scale.Clefs.F) {
            offset = 2;
        }
        // draw #
        for(int i = 1; i <= signature; i++) {
            putDrawable(80 + i * 20, score_offset_y + 5 * 20 - (accidentalsPositions[i + 7] - offset) * 10 + 1, sharp, canvas);
        }
        // draw b
        for(int i = -1; i >= signature; i--) {
            putDrawable(80 - i * 20, score_offset_y + 5 * 20 - (accidentalsPositions[i + 7] - offset) * 10 + 1, flat, canvas);
        }
    }

    void drawNote(Canvas canvas)
    {
        if (activity == null || activity.app == null) {
            return;
        }

        int position = activity.app.notePosition();

        // debug("pos " + Integer.toString(position) + " sh" + Integer.toString(score_height), canvas);
        int y = score_offset_y + 2 + 5 * 20 - position * 10;
        putDrawable(note_position, y, note, canvas);
        switch (activity.app.note.accidentals()) {
            case SHARP:
                putDrawable(note_position - 40, y, sharp, canvas);
                break;
            case RELEASE:
                putDrawable(note_position - 40, y, natural, canvas);
                break;
            case FLAT:
                putDrawable(note_position - 40, y, flat, canvas);
                break;
        }
        /* lines bellow */
        for (int a = 0; a >= position; a -= 2) {
            drawLine(note_position - 35, score_offset_y + 5 * 20 - a * 10 + 1, 70, 2, canvas);
        }

        /* lines above */
        for (int a = 12; a <= position; a += 2) {
            drawLine(note_position - 35, score_offset_y - (a - 11) * 10 - 10 + 1, 70, 2, canvas);
        }

    }

    void drawRect(Rect r, Canvas c)
    {
        drawLine(r.left, r.top, r.right - r.left, 2, c);
        drawLine(r.right, r.top, 2, r.bottom - r.top, c);
    }

    void drawButtons(Canvas canvas)
    {
        putDrawable(buttonPositions.get(Buttons.UP).centerX(), buttonPositions.get(Buttons.UP).centerY(), arrowUp, canvas, 1.3);
        putDrawable(buttonPositions.get(Buttons.DOWN).centerX(), buttonPositions.get(Buttons.DOWN).centerY(), arrowDown, canvas, 1.3);
        putDrawable(buttonPositions.get(Buttons.HALFUP).centerX(), buttonPositions.get(Buttons.UP).centerY(), arrowUp, canvas, 1.0);
        putDrawable(buttonPositions.get(Buttons.HALFDOWN).centerX(), buttonPositions.get(Buttons.DOWN).centerY(), arrowDown, canvas, 1.0);
        putDrawable(buttonPositions.get(Buttons.SCALEUP).centerX(), buttonPositions.get(Buttons.UP).centerY(), arrowUp, canvas, 1.3);
        putDrawable(buttonPositions.get(Buttons.SCALEDOWN).centerX(), buttonPositions.get(Buttons.DOWN).centerY(), arrowDown, canvas, 1.3);
    }

    private void calculateScale() {
        double sh, sv;
        double height, width;

        width = getWidth();
        height = getHeight();
        sh = width / score_width;
        sv = height / score_height;
        scalefactor = (sv < sh) ? sv : sh;
        if (sh < sv) {
            scalefactor = sh;
            score_center_x = 0;
            score_center_y = (int) (height - width) / 2;
        } else {
            scalefactor = sv;
            score_center_y = 0;
            score_center_x = (int) (width - height) / 2;
        }
    }

    private void drawClef(Canvas canvas)
    {
        if (activity.app.clef() == Scale.Clefs.G) {
            putDrawable(score_offset_x + 40, score_offset_y + 4 * 20, gclef, canvas);
        } else {
            putDrawable(score_offset_x + 40, score_offset_y + 20, fclef, canvas);
        }
    }

    protected void onDraw(Canvas canvas)
    {
        calculateScale();
        drawClef(canvas);
        drawScoreLines(canvas);
        drawSignature(canvas);
        drawNote(canvas);
        drawButtons(canvas);
    }

    void setGripView(View v)
    {
        grip = v;
    }

    private void invalidateGripView() {
        if (grip != null) {
            grip.invalidate();
        }
    }

    private void noteUp()
    {
        activity.app.noteUp();
        invalidate();
        invalidateGripView();
    }

    private void noteDown()
    {
        activity.app.noteDown();
        invalidate();
        invalidateGripView();
    }
    private void noteUpHalf()
    {
        activity.app.noteUpHalf();
        invalidate();
        invalidateGripView();
    }

    private void noteDownHalf()
    {
        activity.app.noteDownHalf();
        invalidate();
        invalidateGripView();
    }

    private void scaleUp()
    {
        activity.app.signatureUp();
        invalidate();
        invalidateGripView();
    }

    private void scaleDown()
    {
        activity.app.signatureDown();
        invalidate();
        invalidateGripView();
    }

    private void setNote(int y)
    {
        int position = (score_offset_y + 2 + 5 * 20 - y + 3) / 10;
        activity.app.noteByPosition(position);
        invalidate();
        invalidateGripView();
    }
}
