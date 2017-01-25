package com.cloudoki.demo3.googlecloud;

/**
 * Created by rmalta on 25/01/2017.
 */

public interface IResults {

    void onPartial(String text);

    void onFinal(String text);
}
