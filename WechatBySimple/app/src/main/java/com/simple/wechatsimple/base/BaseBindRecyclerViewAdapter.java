package com.simple.wechatsimple.base;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.List;

public abstract class BaseBindRecyclerViewAdapter<T> extends RecyclerView.Adapter {

    public List<T> mList; //数据源
    public LayoutInflater inflater;

    public BaseBindRecyclerViewAdapter(Context context, List<T> mList) {
        this.mList = mList;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return onCreateMyViewHolder(parent,viewType);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        onBindMyViewHolder(holder, position);
    }

    //获取Item布局
    public abstract RecyclerView.ViewHolder onCreateMyViewHolder(ViewGroup parent, int viewType);

    //绑定数据
    public abstract void onBindMyViewHolder(RecyclerView.ViewHolder holder, int position);
}
