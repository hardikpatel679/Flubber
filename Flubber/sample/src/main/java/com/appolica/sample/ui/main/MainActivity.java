package com.appolica.sample.ui.main;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.appolica.flubber.AnimationBody;
import com.appolica.flubber.Flubber;
import com.appolica.sample.R;
import com.appolica.sample.databinding.ActivityMainBinding;
import com.appolica.sample.ui.animation.CustomAnimationBody;
import com.appolica.sample.ui.animation.FABRevealProvider;
import com.appolica.sample.ui.animation.RevealProvider;
import com.appolica.sample.ui.animation.SimpleAnimatorListener;
import com.appolica.sample.ui.editor.EditorFragment;

import java.util.Iterator;
import java.util.List;

public class MainActivity
        extends AppCompatActivity
        implements AddClickListener,
        FlubberClickListener, MainPanelFragment.AnimationClickListener {

    public static final int SECOND = 1000;
    public static final int DURATION_REVEAL = 350;
    public static final int DURATION_FADE = 200;
    public static final String EDITOR_VISIBILITY = "EditorVisibility";
    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        final MainPanelFragment mainFragment = new MainPanelFragment();

        getSupportFragmentManager().beginTransaction()
                .add(R.id.mainPanelContainer, mainFragment, MainPanelFragment.TAG)
                .commitNow();

        mainFragment.setClickListener(this);

        binding.setAddClickListener(this);
        binding.setFlubberClickListener(this);

        restoreState(savedInstanceState);
    }

    private void restoreState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            binding.editorPanelContainer.setVisibility(savedInstanceState.getInt(EDITOR_VISIBILITY));
            if (isEditorOpen()) {
                binding.floatingActionButton.setImageResource(R.drawable.ic_done_white_24dp);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(EDITOR_VISIBILITY, binding.editorPanelContainer.getVisibility());
    }

    @Override
    public void onFlubberClick(View view) {

        final EditorFragment editorFragment =
                (EditorFragment) getSupportFragmentManager().findFragmentByTag(EditorFragment.TAG);
        if (editorFragment != null && editorFragment.isVisible()) {

            editorFragment.onFlubberClick(view);

        } else {
            final MainPanelFragment fragment = getFragment(MainPanelFragment.TAG);
            AnimatorSet allAnimationsSet = buildSetForBodies(fragment.getAnimations(), view);

            if (allAnimationsSet.getChildAnimations().size() > 0) {
                allAnimationsSet.start();
            }
        }
    }

    @Override
    public void onAddClick() {
        if (isEditorOpen()) {
            final MainPanelFragment fragment = getFragment(MainPanelFragment.TAG);
            final EditorFragment editorFragment = getFragment(EditorFragment.TAG);

            fragment.addAnimation(editorFragment.getAnimationBody());

            closeEditor();
        } else {
            openEditor(new CustomAnimationBody());
        }
    }

    @Override
    public void onAnimationClick(CustomAnimationBody animationBody) {
        openEditor(animationBody);
    }

    private AnimatorSet buildSetForBodies(List<AnimationBody> animations, View view) {
        final AnimatorSet animatorSet = new AnimatorSet();

        if (animations.size() > 0) {
            final Iterator<AnimationBody> iterator = animations.iterator();

            final AnimatorSet.Builder builder =
                    animatorSet.play(iterator.next().createFor(view));

            while (iterator.hasNext()) {
                builder.with(iterator.next().createFor(view));
            }
        }

        return animatorSet;
    }

    private void openEditor(CustomAnimationBody animationBody) {

        final EditorFragment editorFragment = new EditorFragment();
        final FragmentManager fragmentManager = getSupportFragmentManager();

        editorFragment.setAnimationBody(animationBody);

        final FragmentTransaction transaction =
                fragmentManager.beginTransaction()
                        .replace(R.id.editorPanelContainer, editorFragment, EditorFragment.TAG);

        binding.mainPanelContainer.setVisibility(View.INVISIBLE);

        final Animator showAnimation = getShowWithReveal(binding.editorPanelContainer);

        Flubber.with()
                .animation(FABRevealProvider.create(R.drawable.ic_done_white_24dp))
                .duration(DURATION_REVEAL)
                .autoStart(true)
                .createFor(binding.floatingActionButton);

        showAnimation.start();

        transaction.commitNow();
    }

    private Animator getShowWithReveal(final View toShow) {
        final AnimatorSet animatorSet = new AnimatorSet();

        toShow.setVisibility(View.INVISIBLE);
        toShow.setAlpha(1);

        final Animator reveal = Flubber.with()
                .animation(RevealProvider.create(binding.floatingActionButton, binding.revealView, true))
                .duration(DURATION_REVEAL)
                .createFor(toShow);

        final Animator fadeOut = Flubber.with()
                .animation(Flubber.AnimationPreset.FADE_OUT)
                .interpolator(Flubber.Curve.BZR_EASE_OUT)
                .duration(DURATION_FADE)
                .createFor(binding.revealView);

        animatorSet.play(fadeOut).after(reveal);

        return animatorSet;
    }

    private void closeEditor() {
        final EditorFragment editorFragment = getFragment(EditorFragment.TAG);

        final FragmentTransaction transaction = getSupportFragmentManager().beginTransaction().remove(editorFragment);
        final AnimatorSet hideAnimation = getHideWithReveal(binding.editorPanelContainer);

        hideAnimation.addListener(new SimpleAnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                transaction.commitNow();
                binding.mainPanelContainer.setVisibility(View.VISIBLE);
            }
        });

        Flubber.with()
                .animation(FABRevealProvider.create(R.drawable.ic_add_white_48dp))
                .autoStart(true)
                .duration(DURATION_REVEAL)
                .createFor(binding.floatingActionButton);

        hideAnimation.start();
    }

    private AnimatorSet getHideWithReveal(final View toHide) {
        final AnimatorSet animatorSet = new AnimatorSet();

        binding.revealView.setVisibility(View.VISIBLE);

        final Animator revealAnimation =
                Flubber.with()
                        .animation(RevealProvider.create(binding.floatingActionButton, binding.revealView, false))
                        .duration(DURATION_REVEAL)
                        .createFor(toHide);

        final Animator fadeOut =
                Flubber.with()
                        .animation(Flubber.AnimationPreset.FADE_IN)
                        .interpolator(Flubber.Curve.BZR_EASE_OUT)
                        .duration(DURATION_FADE)
                        .createFor(binding.revealView);

        fadeOut.addListener(new SimpleAnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                toHide.setVisibility(View.INVISIBLE);
            }
        });

        animatorSet.play(revealAnimation).after(fadeOut);

        return animatorSet;
    }

    private <T extends Fragment> T getFragment(String tag) {
        return (T) getSupportFragmentManager().findFragmentByTag(tag);
    }

    @Override
    public void onBackPressed() {
        if (isEditorOpen()) {
            closeEditor();
        } else {
            super.onBackPressed();
        }
    }

    private boolean isEditorOpen() {
        return binding.editorPanelContainer.getVisibility() == View.VISIBLE;
    }
}
