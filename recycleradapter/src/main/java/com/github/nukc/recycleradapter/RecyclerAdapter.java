package com.github.nukc.recycleradapter;

import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by C on 16/6/27.
 */
public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

    private static final String TAG = RecyclerAdapter.class.getSimpleName();
    private static final byte TYPE_FOOTER = -2;

    private RecyclerView.Adapter mAdapter;
    private View mFooterView;
    private int mFooterResId = View.NO_ID;

    private RecyclerView mRecyclerView;
    private OnLoadMoreListener mOnLoadMoreListener;

    private boolean mLoadMoreEnabled = true;

    public RecyclerAdapter(@NonNull RecyclerView.Adapter adapter) {
        registerAdapter(adapter);
    }

    public RecyclerAdapter(@NonNull RecyclerView.Adapter adapter, View footerView) {
        registerAdapter(adapter);
        mFooterView = footerView;
    }

    public RecyclerAdapter(@NonNull RecyclerView.Adapter adapter, @LayoutRes int resId) {
        registerAdapter(adapter);
        mFooterResId = resId;
    }

    private void registerAdapter(RecyclerView.Adapter adapter) {
        if (adapter == null) {
            throw new NullPointerException("adapter can not be null!");
        }

        mAdapter = adapter;
        mAdapter.registerAdapterDataObserver(mObserver);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_FOOTER) {
            if (mFooterResId != View.NO_ID) {
                mFooterView = LayoutInflater.from(parent.getContext()).inflate(mFooterResId, parent, false);
            }
            if (mFooterView != null) {
                return new FooterHolder(mFooterView);
            }
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.base_footer, parent, false);
            return new FooterHolder(view);
        }

        return mAdapter.onCreateViewHolder(parent, viewType);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof FooterHolder) {
            if (!canScroll()) {
                holder.itemView.setVisibility(View.GONE);
            }
        } else {
            mAdapter.onBindViewHolder(holder, position);
        }
    }

    @Override
    public int getItemCount() {
        int count = mAdapter.getItemCount();
        return count == 0 ? 0 : mLoadMoreEnabled ? count + 1 : count;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == mAdapter.getItemCount() && mLoadMoreEnabled) {
            return TYPE_FOOTER;
        }
        return super.getItemViewType(position);
    }

    public boolean canScroll() {
        return ViewCompat.canScrollVertically(mRecyclerView, -1);
    }

    public void setLoadMoreEnabled(boolean enabled) {
        mLoadMoreEnabled = enabled;
    }

    public boolean getLoadMoreEnabled() {
        return mLoadMoreEnabled;
    }

    public void setFooterView(View footerView) {
        mFooterView = footerView;
    }

    public void setFooterView(@LayoutRes int resId) {
        mFooterResId = resId;
    }

    static class FooterHolder extends RecyclerView.ViewHolder {

        public FooterHolder(View itemView) {
            super(itemView);

            ViewGroup.LayoutParams layoutParams = itemView.getLayoutParams();
            if (layoutParams instanceof StaggeredGridLayoutManager.LayoutParams) {
                ((StaggeredGridLayoutManager.LayoutParams) layoutParams).setFullSpan(true);
            }
        }
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        mRecyclerView = recyclerView;
        recyclerView.addOnScrollListener(mOnScrollListener);

        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (layoutManager instanceof GridLayoutManager) {
            final GridLayoutManager gridLayoutManager = ((GridLayoutManager) layoutManager);
            final GridLayoutManager.SpanSizeLookup originalSizeLookup = gridLayoutManager.getSpanSizeLookup();

            gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    if (getItemViewType(position) == TYPE_FOOTER) {
                        return gridLayoutManager.getSpanCount();
                    } else if (originalSizeLookup != null) {
                        return originalSizeLookup.getSpanSize(position);
                    }

                    return 1;
                }
            });
        }
    }

    private RecyclerView.OnScrollListener mOnScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);

            if (!ViewCompat.canScrollVertically(recyclerView, -1) || !mLoadMoreEnabled) {
                Log.d(TAG, "recyclerView can not scroll!");
                return;
            }

            if (newState == RecyclerView.SCROLL_STATE_IDLE && mOnLoadMoreListener != null) {
                boolean isBottom;
                RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
                if (layoutManager instanceof LinearLayoutManager) {
                    isBottom = ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition()
                            >= layoutManager.getItemCount() - 1;
                } else if (layoutManager instanceof StaggeredGridLayoutManager) {
                    StaggeredGridLayoutManager sgLayoutManager = (StaggeredGridLayoutManager) layoutManager;
                    int[] into = new int[sgLayoutManager.getSpanCount()];
                    sgLayoutManager.findLastVisibleItemPositions(into);

                    isBottom = last(into) >= layoutManager.getItemCount() - 1;
                }else {
                    isBottom = ((GridLayoutManager) layoutManager).findLastVisibleItemPosition()
                            >= layoutManager.getItemCount() - 1;
                }

                if (layoutManager.getItemCount() > 0 && isBottom) {
                    mOnLoadMoreListener.onLoadMore();
                }
            }
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
        }
    };

    //取到最后的一个节点
    private static int last(int[] lastPositions) {
        int last = lastPositions[0];
        for (int value : lastPositions) {
            if (value > last) {
                last = value;
            }
        }
        return last;
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        recyclerView.removeOnScrollListener(mOnScrollListener);
        mAdapter.unregisterAdapterDataObserver(mObserver);
        mRecyclerView = null;
    }


    public void setLoadMoreListener(OnLoadMoreListener listener) {
        mOnLoadMoreListener = listener;
    }

    public interface OnLoadMoreListener{
        void onLoadMore();
    }

    private RecyclerView.AdapterDataObserver mObserver = new RecyclerView.AdapterDataObserver() {
        @Override
        public void onChanged() {
            RecyclerAdapter.this.notifyDataSetChanged();
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            RecyclerAdapter.this.notifyItemRangeChanged(positionStart, itemCount);
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
            RecyclerAdapter.this.notifyItemRangeChanged(positionStart, itemCount, payload);
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            RecyclerAdapter.this.notifyItemRangeInserted(positionStart, itemCount);
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            RecyclerAdapter.this.notifyItemRangeRemoved(positionStart, itemCount);
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            RecyclerAdapter.this.notifyItemMoved(fromPosition, toPosition);
        }
    };
}