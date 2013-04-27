package com.smartvision.JxClient;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created with IntelliJ IDEA.
 * User: luhang
 * Date: 4/26/13
 * Time: 4:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class MyEditText extends android.widget.EditText implements View.OnLongClickListener{
    public MyEditText(Context context) {
        super(context);
        this.setOnLongClickListener(this);
    }

    public MyEditText(Context context, AttributeSet attrs) {
        super(context,attrs);
        this.setOnLongClickListener(this);
    }

    public MyEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.setOnLongClickListener(this);
    }

    @Override
    public boolean onLongClick(View v) {
        this.selectAll();
        return true;
    }
}
