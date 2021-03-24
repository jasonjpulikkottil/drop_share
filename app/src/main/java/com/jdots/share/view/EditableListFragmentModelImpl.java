package com.jdots.share.view;

import com.jdots.share.fragment.EditableListFragment;
import com.jdots.share.widget.EditableListAdapter;

public interface EditableListFragmentModelImpl<V extends EditableListAdapter.EditableViewHolder>
{
    void setLayoutClickListener(EditableListFragment.LayoutClickListener<V> clickListener);
}
