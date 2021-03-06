package org.yczbj.ycrefreshviewlib.adapter;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.View;
import android.view.ViewGroup;

import org.yczbj.ycrefreshviewlib.inter.InterEventDelegate;
import org.yczbj.ycrefreshviewlib.inter.InterItemView;
import org.yczbj.ycrefreshviewlib.inter.OnErrorListener;
import org.yczbj.ycrefreshviewlib.inter.OnItemChildClickListener;
import org.yczbj.ycrefreshviewlib.inter.OnItemClickListener;
import org.yczbj.ycrefreshviewlib.inter.OnItemLongClickListener;
import org.yczbj.ycrefreshviewlib.inter.OnLoadMoreListener;
import org.yczbj.ycrefreshviewlib.inter.OnMoreListener;
import org.yczbj.ycrefreshviewlib.inter.OnNoMoreListener;
import org.yczbj.ycrefreshviewlib.utils.RecyclerUtils;
import org.yczbj.ycrefreshviewlib.utils.RefreshLogUtils;
import org.yczbj.ycrefreshviewlib.holder.BaseViewHolder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


/**
 * <pre>
 *     @author 杨充
 *     blog  : https://github.com/yangchong211
 *     time  : 2017/5/2
 *     desc  : 自定义adapter
 *     revise: 注意这里使用泛型数据类型
 * </pre>
 */
public abstract class RecyclerArrayAdapter<T> extends RecyclerView.Adapter<BaseViewHolder>   {

    private List<T> mObjects;
    private InterEventDelegate mEventDelegate;
    private ArrayList<InterItemView> headers = new ArrayList<>();
    private ArrayList<InterItemView> footers = new ArrayList<>();
    private OnItemClickListener mItemClickListener;
    private OnItemLongClickListener mItemLongClickListener;
    private OnItemChildClickListener mOnItemChildClickListener;
    private final Object mLock = new Object();
    private boolean mNotifyOnChange = true;
    private Context mContext;


    public RecyclerArrayAdapter(Context context) {
        RecyclerUtils.checkContent(context);
        init(context,  new ArrayList<T>());
    }


    public RecyclerArrayAdapter(Context context, T[] objects) {
        RecyclerUtils.checkContent(context);
        init(context, Arrays.asList(objects));
    }


    public RecyclerArrayAdapter(Context context, List<T> objects) {
        RecyclerUtils.checkContent(context);
        init(context, objects);
    }


    private void init(Context context , List<T> objects) {
        mContext = context;
        mObjects = new ArrayList<>(objects);
    }

    /**
     * 创建viewHolder
     * @param parent                        parent
     * @param viewType                      type类型
     * @return
     */
    @NonNull
    @Override
    public final BaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = createSpViewByType(parent, viewType);
        if (view!=null){
            return new BaseViewHolder(view);
        }
        final BaseViewHolder viewHolder = OnCreateViewHolder(parent, viewType);
        setOnClickListener(viewHolder);
        return viewHolder;
    }

    /**
     * 获取类型
     * @param position                      索引
     * @return                              int
     */
    @Deprecated
    @Override
    public final int getItemViewType(int position) {
        if (headers.size()!=0){
            if (position<headers.size()) {
                return headers.get(position).hashCode();
            }
        }
        if (footers.size()!=0){
            /*
            eg:
            0:header1
            1:header2   2
            2:object1
            3:object2
            4:object3
            5:object4
            6:footer1   6(position) - 2 - 4 = 0
            7:footer2
             */
            int i = position - headers.size() - mObjects.size();
            if (i >= 0){
                return footers.get(i).hashCode();
            }
        }
        return getViewType(position-headers.size());
    }


    public int getViewType(int position){
        return 0;
    }

    /**
     * 这个函数包含了头部和尾部view的个数，不是真正的item个数。
     * 包含item+header头布局数量+footer底布局数量
     */
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    @Override
    public final int getItemCount() {
        return mObjects.size() + headers.size() + footers.size();
    }


    /**
     * 绑定viewHolder
     * @param holder                        holder
     * @param position                      索引
     */
    @Override
    public final void onBindViewHolder(BaseViewHolder holder, int position) {
        holder.itemView.setId(position);
        if (headers.size()!=0 && position<headers.size()){
            headers.get(position).onBindView(holder.itemView);
            return ;
        }

        int i = position - headers.size() - mObjects.size();
        if (footers.size()!=0 && i>=0){
            footers.get(i).onBindView(holder.itemView);
            return ;
        }


        OnBindViewHolder(holder,position-headers.size());
    }

    @Override
    public long getItemId(int position) {
        return position;
    }


    /**---------------------------------子类需要重写的方法---------------------------------------*/


    /**
     * 抽象方法，子类继承
     */
    public abstract BaseViewHolder OnCreateViewHolder(ViewGroup parent, int viewType);


    @SuppressWarnings("unchecked")
    private void OnBindViewHolder(BaseViewHolder holder, final int position){
        holder.setData(getItem(position));
    }

    /**
     *
     * @param maxCount
     * @return
     */
    public GridSpanSizeLookup obtainGridSpanSizeLookUp(int maxCount){
        return new GridSpanSizeLookup(maxCount,headers,footers,mObjects);
    }


    /**
     * 停止加载更多
     */
    public void stopMore(){
        if (mEventDelegate == null) {
            throw new NullPointerException("You should invoking setLoadMore() first");
        }
        mEventDelegate.stopLoadMore();
    }

    /**
     * 暂停加载更多
     */
    public void pauseMore(){
        if (mEventDelegate == null) {
            throw new NullPointerException("You should invoking setLoadMore() first");
        }
        mEventDelegate.pauseLoadMore();
    }

    /**
     * 恢复加载更多
     */
    public void resumeMore(){
        if (mEventDelegate == null) {
            throw new NullPointerException("You should invoking setLoadMore() first");
        }
        mEventDelegate.resumeLoadMore();
    }

    /**
     * 添加headerView
     * @param view                      view
     */
    public void addHeader(InterItemView view){
        if (view==null) {
            throw new NullPointerException("InterItemView can't be null");
        }
        headers.add(view);
        notifyItemInserted(headers.size()-1);
    }


    /**
     * 添加footerView
     * @param view                      view
     */
    public void addFooter(InterItemView view){
        if (view==null) {
            throw new NullPointerException("InterItemView can't be null");
        }
        footers.add(view);
        notifyItemInserted(headers.size()+getCount()+footers.size()-1);
    }

    /**
     * 清除所有header
     */
    public void removeAllHeader(){
        int count = headers.size();
        headers.clear();
        notifyItemRangeRemoved(0,count);
    }

    /**
     * 清除所有footer
     */
    public void removeAllFooter(){
        int count = footers.size();
        footers.clear();
        notifyItemRangeRemoved(headers.size()+getCount(),count);
    }

    /**
     * 获取某个索引处的headerView
     * @param index                 索引
     * @return                      InterItemView
     */
    public InterItemView getHeader(int index){
        return headers.get(index);
    }

    /**
     * 获取某个索引处的footerView
     * @param index                 索引
     * @return                      InterItemView
     */
    public InterItemView getFooter(int index){
        return footers.get(index);
    }

    /**
     * 获取header的数量
     * @return                      数量
     */
    public int getHeaderCount(){return headers.size();}

    /**
     * 获取footer的数量
     * @return                      数量
     */
    public int getFooterCount(){return footers.size();}

    /**
     * 移除某个headerView
     * @param view                  view
     */
    public void removeHeader(InterItemView view){
        int position = headers.indexOf(view);
        headers.remove(view);
        notifyItemRemoved(position);
    }

    /**
     * 移除某个footerView
     * @param view                  view
     */
    public void removeFooter(InterItemView view){
        int position = headers.size()+getCount()+footers.indexOf(view);
        footers.remove(view);
        notifyItemRemoved(position);
    }


    private InterEventDelegate getEventDelegate(){
        if (mEventDelegate == null) {
            mEventDelegate = new DefaultEventDelegate(this);
        }
        return mEventDelegate;
    }


    /**
     * 设置上拉加载更多的自定义布局和监听
     * @param res                   res布局
     * @param listener              listener
     */
    public void setMore(final int res, final OnLoadMoreListener listener){
        getEventDelegate().setMore(res, new OnMoreListener() {
            @Override
            public void onMoreShow() {
                listener.onLoadMore();
            }

            @Override
            public void onMoreClick() {

            }
        });
    }


    /**
     * 设置上拉加载更多的自定义布局和监听
     * @param view                  view布局
     * @param listener              listener
     */
    public void setMore(final View view,final OnLoadMoreListener listener){
        getEventDelegate().setMore(view, new OnMoreListener() {
            @Override
            public void onMoreShow() {
                listener.onLoadMore();
            }

            @Override
            public void onMoreClick() {

            }
        });
    }

    /**
     * 设置上拉加载更多的自定义布局和
     * @param res                   res布局
     * @param listener              listener
     */
    public void setMore(final int res, final OnMoreListener listener){
        getEventDelegate().setMore(res, listener);
    }

    /**
     * 设置上拉加载更多的自定义布局和
     * @param view                  view布局
     * @param listener              listener
     */
    public void setMore(final View view,OnMoreListener listener){
        getEventDelegate().setMore(view, listener);
    }

    /**
     * 设置上拉加载没有更多数据布局
     * @param res                   res布局
     */
    public void setNoMore(final int res) {
        getEventDelegate().setNoMore(res,null);
    }

    /**
     * 设置上拉加载没有更多数据布局
     * @param view                  没有更多数据布局view
     */
    public void setNoMore(final View view) {
        getEventDelegate().setNoMore(view,null);
    }

    /**
     * 设置上拉加载没有更多数据监听
     * @param view                  没有更多数据布局
     * @param listener              上拉加载没有更多数据监听
     */
    public void setNoMore(final View view , OnNoMoreListener listener) {
        getEventDelegate().setNoMore(view,listener);
    }

    /**
     * 设置上拉加载没有更多数据监听
     * @param res                   没有更多数据布局res
     * @param listener              上拉加载没有更多数据监听
     */
    public void setNoMore(final @LayoutRes int res , OnNoMoreListener listener) {
        getEventDelegate().setNoMore(res,listener);
    }


    /**
     * 设置上拉加载异常的布局
     * @param res                   view
     */
    public void setError(final @LayoutRes int res) {
        getEventDelegate().setErrorMore(res,null);
    }

    /**
     * 设置上拉加载异常的布局
     * @param view                  view
     */
    public void setError(final View view) {
        getEventDelegate().setErrorMore(view,null);
    }

    /**
     * 设置上拉加载异常的布局和异常监听
     * @param res                   view
     * @param listener              上拉加载更多异常监听
     */
    public void setError(final @LayoutRes int res,OnErrorListener listener) {
        getEventDelegate().setErrorMore(res,listener);
    }

    public void setError(final View view,OnErrorListener listener) {
        getEventDelegate().setErrorMore(view,listener);
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        //增加对RecyclerArrayAdapter奇葩操作的修复措施
        registerAdapterDataObserver(new FixDataObserver(recyclerView));
    }

    private class FixDataObserver extends RecyclerView.AdapterDataObserver {

        private RecyclerView recyclerView;
        FixDataObserver(RecyclerView recyclerView) {
            this.recyclerView = recyclerView;
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            if (recyclerView.getAdapter() instanceof RecyclerArrayAdapter) {
                RecyclerArrayAdapter adapter = (RecyclerArrayAdapter) recyclerView.getAdapter();
                if (adapter.getFooterCount() > 0 && adapter.getCount() == itemCount) {
                    recyclerView.scrollToPosition(0);
                }
            }
        }
    }

    /**
     * 添加数据
     * @param object            数据
     */
    public void add(T object) {
        if (mEventDelegate!=null) {
            mEventDelegate.addData(object == null ? 0 : 1);
        }
        if (object!=null){
            synchronized (mLock) {
                mObjects.add(object);
            }
        }
        if (mNotifyOnChange) {
            notifyItemInserted(headers.size() + getCount());
        }
        RefreshLogUtils.d("add notifyItemInserted "+(headers.size()+getCount()));
    }

    /**
     * 添加所有数据
     * @param collection        Collection集合数据
     */
    public void addAll(Collection<? extends T> collection) {
        if (mEventDelegate!=null) {
            mEventDelegate.addData(collection == null ? 0 : collection.size());
        }
        if (collection!=null&&collection.size()!=0){
            synchronized (mLock) {
                mObjects.addAll(collection);
            }
        }
        int dataCount = collection==null?0:collection.size();
        if (mNotifyOnChange) {
            notifyItemRangeInserted(headers.size() + getCount() - dataCount, dataCount);
        }
        RefreshLogUtils.d("addAll notifyItemRangeInserted "+(headers.size()+getCount()-dataCount)+","+(dataCount));

    }

    /**
     * 添加所有数据
     * @param items            数据
     */
    public void addAll(T[] items) {
        if (mEventDelegate!=null) {
            mEventDelegate.addData(items == null ? 0 : items.length);
        }
        if (items!=null&&items.length!=0) {
            synchronized (mLock) {
                Collections.addAll(mObjects, items);
            }
        }
        int dataCount = items==null?0:items.length;
        if (mNotifyOnChange) {
            notifyItemRangeInserted(headers.size() + getCount() - dataCount, dataCount);
        }
        RefreshLogUtils.d("addAll notifyItemRangeInserted "+((headers.size()+getCount()-dataCount)+","+(dataCount)));
    }

    /**
     * 插入，不会触发任何事情
     * @param object            数据
     * @param index             索引
     */
    public void insert(T object, int index) {
        synchronized (mLock) {
            mObjects.add(index, object);
        }
        if (mNotifyOnChange) {
            notifyItemInserted(headers.size() + index);
        }
        RefreshLogUtils.d("insert notifyItemRangeInserted "+(headers.size()+index));
    }

    /**
     * 插入数组，不会触发任何事情
     * @param object            数据
     * @param index             索引
     */
    public void insertAll(T[] object, int index) {
        synchronized (mLock) {
            mObjects.addAll(index, Arrays.asList(object));
        }
        int dataCount = object.length;
        if (mNotifyOnChange) {
            notifyItemRangeInserted(headers.size() + index, dataCount);
        }
        RefreshLogUtils.d("insertAll notifyItemRangeInserted "+((headers.size()+index)+","+(dataCount)));
    }

    /**
     * 插入数组，不会触发任何事情
     * @param object            数据
     * @param index             索引
     */
    public void insertAll(Collection<? extends T> object, int index) {
        synchronized (mLock) {
            mObjects.addAll(index, object);
        }
        int dataCount = object.size();
        if (mNotifyOnChange) {
            notifyItemRangeInserted(headers.size() + index, dataCount);
        }
        RefreshLogUtils.d("insertAll notifyItemRangeInserted "+((headers.size()+index)+","+(dataCount)));
    }


    /**
     * 更新数据
     * @param object            数据
     * @param pos               索引
     */
    public void update(T object,int pos){
        synchronized (mLock) {
            mObjects.set(pos,object);
        }
        if (mNotifyOnChange) {
            notifyItemChanged(pos);
        }
        RefreshLogUtils.d("insertAll notifyItemChanged "+pos);
    }


    /**
     * 删除，不会触发任何事情
     * @param object            要移除的数据
     */
    public void remove(T object) {
        int position = mObjects.indexOf(object);
        synchronized (mLock) {
            if (mObjects.remove(object)){
                if (mNotifyOnChange) {
                    notifyItemRemoved(headers.size() + position);
                }
                RefreshLogUtils.d("remove notifyItemRemoved "+(headers.size()+position));
            }
        }
    }


    /**
     * 将某个索引处的数据置顶
     * @param position            要移除数据的索引
     */
    public void setTop(int position){
        T t;
        synchronized (mLock) {
            t = mObjects.get(position);
            mObjects.remove(position);
        }
        if (mNotifyOnChange) {
            notifyItemInserted(headers.size());
        }
        mObjects.add(0,t);
        if (mNotifyOnChange) {
            notifyItemRemoved(headers.size() + 1);
        }
        RefreshLogUtils.d("remove notifyItemRemoved "+(headers.size()+1));
    }


    /**
     * 删除，不会触发任何事情
     * @param position          要移除数据的索引
     */
    public void remove(int position) {
        synchronized (mLock) {
            mObjects.remove(position);
        }
        if (mNotifyOnChange) {
            notifyItemRemoved(headers.size() + position);
        }
        RefreshLogUtils.d("remove notifyItemRemoved "+(headers.size()+position));
    }


    /**
     * 触发清空
     * 与{@link #clear()}的不同仅在于这个使用notifyItemRangeRemoved.
     */
    public void removeAll() {
        int count = mObjects.size();
        if (mEventDelegate!=null) {
            mEventDelegate.clear();
        }
        synchronized (mLock) {
            mObjects.clear();
        }
        if (mNotifyOnChange) {
            notifyItemRangeRemoved(headers.size(), count);
        }
        RefreshLogUtils.d("clear notifyItemRangeRemoved "+(headers.size())+","+(count));
    }

    /**
     * 触发清空所有的数据
     */
    public void clear() {
        int count = mObjects.size();
        if (mEventDelegate!=null) {
            mEventDelegate.clear();
        }
        synchronized (mLock) {
            mObjects.clear();
        }
        if (mNotifyOnChange) {
            notifyDataSetChanged();
        }
        RefreshLogUtils.d("clear notifyItemRangeRemoved "+(headers.size())+","+(count));
    }

    /**
     * 使用指定的比较器对此适配器的内容进行排序
     */
    public void sort(Comparator<? super T> comparator) {
        synchronized (mLock) {
            Collections.sort(mObjects, comparator);
        }
        if (mNotifyOnChange) {
            notifyDataSetChanged();
        }
    }

    /**
     * 设置操作数据[增删改查]后，是否刷新adapter
     * @param notifyOnChange                默认是刷新的true
     */
    public void setNotifyOnChange(boolean notifyOnChange) {
        mNotifyOnChange = notifyOnChange;
    }

    /**
     * 获取上下文
     * @return
     */
    public Context getContext() {
        return mContext;
    }

    /**
     * 应该使用这个获取item个数
     */
    public int getCount(){
        return mObjects.size();
    }

    private View createSpViewByType(ViewGroup parent, int viewType){
        for (InterItemView headerView : headers){
            if (headerView.hashCode() == viewType){
                View view = headerView.onCreateView(parent);
                StaggeredGridLayoutManager.LayoutParams layoutParams;
                if (view.getLayoutParams()!=null) {
                    layoutParams = new StaggeredGridLayoutManager.LayoutParams(view.getLayoutParams());
                } else {
                    layoutParams = new StaggeredGridLayoutManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                }
                layoutParams.setFullSpan(true);
                view.setLayoutParams(layoutParams);
                return view;
            }
        }
        for (InterItemView footerView : footers){
            if (footerView.hashCode() == viewType){
                View view = footerView.onCreateView(parent);
                StaggeredGridLayoutManager.LayoutParams layoutParams;
                if (view.getLayoutParams()!=null) {
                    layoutParams = new StaggeredGridLayoutManager.LayoutParams(view.getLayoutParams());
                } else {
                    layoutParams = new StaggeredGridLayoutManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                }
                layoutParams.setFullSpan(true);
                view.setLayoutParams(layoutParams);
                return view;
            }
        }
        return null;
    }

    /**
     * 获取所有的数据list集合
     * @return                      list结合
     */
    public List<T> getAllData(){
        return new ArrayList<>(mObjects);
    }

    /**
     * 获取item
     */
    protected T getItem(int position) {
        return mObjects.get(position);
    }

    /**
     * 获取item索引位置
     * @param item                  item
     * @return                      索引位置
     */
    public int getPosition(T item) {
        return mObjects.indexOf(item);
    }


    public class GridSpanSizeLookup extends GridLayoutManager.SpanSizeLookup{

        private int mMaxCount;
        private ArrayList<InterItemView> headers;
        private ArrayList<InterItemView> footers;
        private List<T> mObjects;

        GridSpanSizeLookup(int maxCount, ArrayList<InterItemView> headers,
                           ArrayList<InterItemView> footers, List<T> mObjects){
            this.mMaxCount = maxCount;
            this.headers = headers;
            this.footers = footers;
            this.mObjects = mObjects;
        }

        @Override
        public int getSpanSize(int position) {
            if (headers.size()!=0){
                if (position<headers.size()) {
                    return mMaxCount;
                }
            }
            if (footers.size()!=0) {
                int i = position - headers.size() - mObjects.size();
                if (i >= 0) {
                    return mMaxCount;
                }
            }
            return 1;
        }
    }



    /**---------------------------------点击事件---------------------------------------------------*/

    private void setOnClickListener(final BaseViewHolder viewHolder) {
        //itemView 的点击事件
        if (mItemClickListener!=null) {
            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mItemClickListener.onItemClick(
                            viewHolder.getAdapterPosition()-headers.size());
                }
            });
        }
        if (mItemLongClickListener!=null){
            viewHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    return mItemLongClickListener.onItemLongClick(
                            viewHolder.getAdapterPosition()-headers.size());
                }
            });
        }
    }

    /**
     * 设置条目点击事件
     * @param listener              监听器
     */
    public void setOnItemClickListener(OnItemClickListener listener){
        this.mItemClickListener = listener;
    }

    /**
     * 设置条目长按事件
     * @param listener              监听器
     */
    public void setOnItemLongClickListener(OnItemLongClickListener listener){
        this.mItemLongClickListener = listener;
    }

    /**
     * 设置孩子点击事件
     * @param listener              监听器
     */
    public void setOnItemChildClickListener(OnItemChildClickListener listener) {
        this.mOnItemChildClickListener = listener;
    }

    public OnItemChildClickListener getOnItemChildClickListener() {
        return mOnItemChildClickListener;
    }


}
