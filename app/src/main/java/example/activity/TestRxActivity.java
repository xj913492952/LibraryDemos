package example.activity;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.view.View;

import com.style.base.BaseActivity;
import com.style.framework.R;
import com.style.framework.databinding.ActivityTestRxBinding;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class TestRxActivity extends BaseActivity {

    ActivityTestRxBinding bd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bd = DataBindingUtil.setContentView(this, R.layout.activity_test_rx);
        super.setContentView(bd.getRoot());
        initData();
    }

    @Override
    public void initData() {

    }

    public void skip1(View v) {
        String[] str = {"one", "two", "three", "four", "five"};
        Observable.fromArray(str)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(@NonNull String s) {

                    }

                    @Override
                    public void onError(@NonNull Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
        //Flowable是RxJava2.x中新增的，专门用于应对背压（Backpressure）问题。所谓背压，即生产者的速度大于消费者的速度带来的问题，
        // 比如在Android中常见的点击事件，点击过快则经常会造成点击两次的效果。其中，Flowable默认队列大小为128.

        Flowable.just("hello1", "hello2", "hello3", "hello4", "hello5", "hello6")
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        System.out.println(s);
                    }
                }, new Consumer<Throwable>() {//错误处理消费者
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        /**
                         * Consumer是简易版的Observer，他有多重重载，可以自定义你需要处理的信息，我这里调用的是只接受onNext消息的方法，
                         * 他只提供一个回调接口accept，由于没有onError和onCompete，无法再 接受到onError或者onCompete之后，实现函数回调。
                         * 无法回调，并不代表不接收，他还是会接收到onCompete和onError之后做出默认操作，也就是监听者（Consumer）不在接收
                         * Observable发送的消息，下方的代码测试了该效果。
                         */
                        //do something when error!
                        //code B
                        //注意：当mDisposable.isCanceled()时抛出的异常，这里不会补货，因为已经取消了订阅
                    }
                });
    }

    public void skip2(View v) {
        Flowable.just("hello RxJava 2")
                .subscribeOn(Schedulers.newThread())
                .map(new Function<String, Integer>() {
                    @Override
                    public Integer apply(String s) throws Exception {
                        return s.hashCode();
                    }
                })
                .observeOn(Schedulers.io())
                .map(new Function<Integer, String>() {
                    @Override
                    public String apply(Integer value) throws Exception {
                        return "hashCode=" + value;
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        System.out.println(s);
                    }
                });
    }

    public void skip3(View v) {
        Student[] students = new Student[3];
        for (int i = 0; i < 3; i++) {
            students[i] = new Student();
            students[i].name = "Student" + i;
            Course[] courses = new Course[10];
            for (int j = 0; j < 10; j++) {
                courses[j] = new Course();
                courses[j].name = students[i].name + "->" + "course" + j;
            }
            students[i].courses = courses;
        }
        Observable.fromArray(students)
                .subscribeOn(Schedulers.io())
                //自动循环二维数组
                .flatMap(new Function<Student, Observable<Course>>() {
                    @Override
                    public Observable<Course> apply(@NonNull Student s) throws Exception {
                        return Observable.fromArray(s.courses);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Course>() {
                    @Override
                    public void accept(Course s) throws Exception {
                        System.out.println(s.name);
                    }
                });
    }

    static class Student {
        String name;
        Course[] courses;

    }

    static class Course {
        String name;
    }
}
