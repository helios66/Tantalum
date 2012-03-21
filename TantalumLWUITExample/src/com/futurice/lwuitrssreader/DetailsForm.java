/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.futurice.lwuitrssreader;

import com.futurice.tantalum2.DefaultResult;
import com.futurice.tantalum2.net.StaticWebCache;
import com.futurice.tantalum2.rms.ImageTypeHandler;
import com.sun.lwuit.Command;
import com.sun.lwuit.Form;
import com.sun.lwuit.Image;
import com.sun.lwuit.Label;
import com.sun.lwuit.animations.CommonTransitions;
import com.sun.lwuit.events.ActionEvent;
import com.sun.lwuit.events.ActionListener;
import java.util.Vector;

/**
 *
 * @author tsaa
 */
public class DetailsForm extends Form implements ActionListener {

    static Command linkCommand = new Command("Open link");
    static Command backCommand = new Command("Back");
    private Label pubDateLabel;
    private Label imgLabel;
    private Vector titleLabels;
    private Vector descriptionLabels;
    private Vector linkLabels;
    private RSSReader midlet;
    private RSSItem current;
    private final StaticWebCache imageCache;

    public DetailsForm(String title, RSSReader midlet) {
        super(title);
        this.midlet = midlet;
        this.imageCache = new StaticWebCache("imgs", '1', new ImageTypeHandler());
        setScrollableY(true);
        pubDateLabel = new Label("");

        pubDateLabel.getStyle().setFont(RSSReader.italicFont);

        imgLabel = new Label("");


        addCommand(linkCommand);
        addCommand(backCommand);

        setTransitionOutAnimator(
                CommonTransitions.createSlide(
                CommonTransitions.SLIDE_HORIZONTAL, true, 200));

        addCommandListener(this);
    }

    public void actionPerformed(ActionEvent ae) {
        String cmdStr = ae.getCommand().getCommandName();

        if (cmdStr.equals("Open link")) {
            try {
                midlet.platformRequest(current.getLink());
            } catch (Exception e) {
                RSSReader.showDialog("Couldn't open link");
            }
        }
        if (cmdStr.equals("Back")) {
            midlet.getListForm().show();
        }
    }

    public void setCurrentRSSItem(final RSSItem item) {
        current = item;
        removeAll();

        titleLabels = getLabels(item.getTitle(), RSSReader.mediumFont, RSSReader.SCREEN_WIDTH);
        pubDateLabel.setText(item.getPubDate());
        descriptionLabels = getLabels(item.getDescription(), RSSReader.plainFont, RSSReader.SCREEN_WIDTH);
        linkLabels = getLabels(item.getLink(), RSSReader.underlinedFont, RSSReader.SCREEN_WIDTH);
        imgLabel = new Label("");
        addLabels(titleLabels);
        addComponent(pubDateLabel);
        addLabels(descriptionLabels);
        addComponent(imgLabel);

        imageCache.get(item.getImgSrc(), new DefaultResult() {
            public void run() {
                super.run();
                
                item.setImg((Image) getResult());
                midlet.getDetailsForm().repaintImg();
            }
        });

        addLabels(linkLabels);
        setScrollY(0);
    }

    public void repaintImg() {
        if (current.getImg() != null) {
            imgLabel.setIcon(current.getImg());
            if (this.contains(imgLabel)) {
                imgLabel.repaint();
            }
        }
    }

    public Vector getLabels(String str, com.sun.lwuit.Font font, int width) {
        Vector labels = new Vector();
        Vector lines = StringUtils.splitToLines(str, font, width);
        for (int i = 0; i < lines.size(); i++) {
            labels.addElement(new Label((String) lines.elementAt(i)));
            ((Label) labels.lastElement()).getStyle().setFont(font);
            ((Label) labels.lastElement()).setGap(0);
        }
        return labels;
    }

    public void addLabels(Vector labels) {
        for (int i = 0; i < labels.size(); i++) {
            addComponent((Label) labels.elementAt(i));
        }
    }
}