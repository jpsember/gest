package com.js.gestApp;

import com.js.android.UITools;
import com.js.basic.Point;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import static android.view.ViewGroup.LayoutParams.*;
import static com.js.basic.Tools.*;

/**
 * <pre>
 * 
 * Class for learning about idiosyncrasies of LinearLayouts
 * 
 * Things learned:
 * 
 * 1) Using WRAP_CONTENT on views without any content is problematic; see buildViewsWithoutContentAndWrap().
 *    Proposed solution: give such a 'no content' view a layout size of zero, and a nonzero weight
 * 
 * 2) The View methods setMinimumWidth/Height() don't seem to have any effect, at least when used in 
 *    conjunction with some LinearLayout configurations; see buildStretchableViewWithMinimumHeight()
 *    
 * </pre>
 * 
 */
public class LayoutExperiments {

  public static View buildExperimentalContentView(Context context) {
    return new LayoutExperiments(context).buildContentView();
  }

  private LayoutExperiments(Context context) {
    mContext = context;
  }

  private Context context() {
    return mContext;
  }

  private LinearLayout linearLayout() {
    LinearLayout view = new LinearLayout(context());
    view.setBackgroundColor(UITools.debugColor());
    return view;
  }

  private View buildContentView() {
    View content;
    switch (7) {
    default:
      content = linearLayout();
      break;
    case 1:
      content = buildDistributeSpaceViaWeights();
      break;
    case 2:
      content = buildNoContentGrabsSpace();
      break;
    case 3:
      content = buildWeightsWithFixedSizeViews();
      break;
    case 4:
      content = buildViewsWithoutContentAndWrap();
      break;
    case 5:
      content = buildViewsWithoutContentAndWrap_Solution();
      break;
    case 6:
      content = buildRowWithStretchingLastElement();
      break;
    case 7:
      content = buildStretchableViewWithMinimumHeight();
      break;
    }

    setOnClickListener(content, "Content");
    return content;
  }

  private static class OurView extends View {

    public OurView(Context context) {
      super(context);

    }

    public OurView setExpectedSize(float w, float h) {
      mExpectedSize = new Point(w, h);
      return this;
    }

    private Point mExpectedSize;

    @Override
    protected void onDraw(Canvas canvas) {
      super.onDraw(canvas);
      onDrawAux(canvas);
    }

    private void onDrawAux(Canvas canvas) {
      Paint p = new Paint();
      p.setColor(Color.WHITE);
      p.setStrokeWidth(3.2f);
      p.setStyle(Paint.Style.STROKE);

      if (mExpectedSize != null) {
        canvas.drawRect(0, 0, mExpectedSize.x, mExpectedSize.y, p);
        canvas.drawLine(0, 0, mExpectedSize.x, mExpectedSize.y, p);
      }
    }
  }

  private void setOnClickListener(View view, final String message) {
    view.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        pr("Clicked on view " + nameOf(v) + " (size=" + v.getWidth() + ","
            + v.getHeight() + "): " + message);
      }
    });
  }

  private OurView view(int color, String message) {
    OurView v = new OurView(context());
    if (color == 0)
      color = UITools.debugColor();
    v.setBackgroundColor(color);
    if (message == null)
      message = "View " + nameOf(v);
    if (message != null)
      setOnClickListener(v, message);
    return v;
  }

  /**
   * Construct a column of two views, each of type View. Neither has any
   * content; we give them explicit small heights, and use weights to distribute
   * the available space to each
   */
  private View buildDistributeSpaceViaWeights() {
    LinearLayout view = linearLayout();
    view.setOrientation(LinearLayout.VERTICAL);

    View v1 = view(Color.RED, "Upper");
    LayoutParams p = params(MATCH_PARENT, 1);
    p.weight = 1.0f;
    view.addView(v1, p);

    View v2 = view(Color.GREEN, "Lower").setExpectedSize(100, 40);
    p = params(MATCH_PARENT, 40);
    p.weight = 0.0f;
    view.addView(v2, p);

    return view;
  }

  /**
   * A View with no content will expand to fill the parent view if it has
   * WRAP_CONTENT set
   */
  private View buildNoContentGrabsSpace() {
    LinearLayout view = linearLayout();
    view.setOrientation(LinearLayout.VERTICAL);

    View v1 = view(Color.RED, "Upper");
    LayoutParams p = params(MATCH_PARENT, WRAP_CONTENT);
    p.weight = 0.0f;
    view.addView(v1, p);

    View v2 = view(Color.GREEN, "Lower").setExpectedSize(200, 40);
    p = params(MATCH_PARENT, 40);
    p.weight = 1.0f;
    view.addView(v2, p);

    return view;
  }

  /**
   * Views with no content and WRAP_CONTENT will have unpredictable sizes.
   * 
   * If you have two such views, their heights will be INVERSELY related to
   * their weights; e.g. the lower weight will produce the taller view
   */
  private View buildViewsWithoutContentAndWrap() {
    LinearLayout view = linearLayout();
    view.setOrientation(LinearLayout.VERTICAL);

    View v1 = view(Color.RED, "Upper");
    LayoutParams p = params(MATCH_PARENT, WRAP_CONTENT);
    p.weight = 1;
    view.addView(v1, p);

    View v2 = view(Color.GREEN, "Lower");
    p = params(MATCH_PARENT, WRAP_CONTENT);
    p.weight = 9;
    view.addView(v2, p);

    return view;
  }

  private LayoutParams params(int width, int height) {
    return new LayoutParams(width, height);
  }

  private View buildViewsWithoutContentAndWrap_Solution() {

    // Give views without content an explicit height of zero, instead of
    // WRAP_CONTENT; their actual heights will be a function of their weights
    LinearLayout view = linearLayout();
    view.setOrientation(LinearLayout.VERTICAL);

    View v1 = view(Color.RED, "Upper");
    LayoutParams p = params(MATCH_PARENT, 0);
    p.weight = 1;
    view.addView(v1, p);

    View v2 = view(Color.GREEN, "Lower");
    p = params(MATCH_PARENT, 0);
    p.weight = 9;
    view.addView(v2, p);

    return view;
  }

  private View buildWeightsWithFixedSizeViews() {
    LinearLayout view = linearLayout();
    view.setOrientation(LinearLayout.VERTICAL);

    View v1 = view(Color.RED, "Upper").setExpectedSize(200, 100);
    LayoutParams p = new LayoutParams(200, 100);
    p.weight = 0;
    view.addView(v1, p);

    View v2 = view(Color.GREEN, "Lower").setExpectedSize(300, 180);
    p = new LayoutParams(300, 180);
    p.weight = 1.0f;
    view.addView(v2, p);

    return view;
  }

  private View buildRowWithStretchingLastElement() {

    LinearLayout view = linearLayout();
    view.setOrientation(LinearLayout.HORIZONTAL);

    for (int i = 0; i < 6; i++) {
      View v = view(0, null);
      LayoutParams p = params(90, MATCH_PARENT);
      p.weight = 0;
      view.addView(v, p);
    }
    {
      View v = view(0, "Stretch");
      LayoutParams p = params(0, MATCH_PARENT);
      p.weight = 1;
      view.addView(v, p);
    }
    return view;
  }

  /**
   * Stretchable views with no content, and one with a minimum height; are
   * resulting sizes equal? No, minimum height is ignored!
   */
  private View buildStretchableViewWithMinimumHeight() {
    LinearLayout view = linearLayout();
    view.setOrientation(LinearLayout.VERTICAL);

    View v1 = view(Color.RED, "Upper");
    LayoutParams p = params(MATCH_PARENT, 0);
    p.weight = 1;
    view.addView(v1, p);

    View v2 = view(Color.GREEN, "Lower");
    p = params(MATCH_PARENT, 0);
    p.weight = 1;
    v2.setMinimumHeight(500);
    view.addView(v2, p);

    return view;
  }

  private static String layoutElement(int n) {
    switch (n) {
    case LayoutParams.MATCH_PARENT:
      return "MATCH_PARENT";
    case LayoutParams.WRAP_CONTENT:
      return "WRAP_CONTENT";
    default:
      return d(n, 11);
    }
  }

  public static String dump(LayoutParams p) {
    StringBuilder sb = new StringBuilder("LayoutParams");
    sb.append(" width:" + layoutElement(p.width));
    sb.append(" height:" + layoutElement(p.height));
    if (p instanceof LinearLayout.LayoutParams) {
      LinearLayout.LayoutParams p2 = (LinearLayout.LayoutParams) p;
      sb.append(" weight:" + d(p2.weight));
    }
    return sb.toString();
  }

  private Context mContext;

}
