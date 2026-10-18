package com.astrolingo.app.today

import com.astrolingo.learn.content.CourseLoader
import org.junit.Assert.assertNotNull
import org.junit.Test

class TodayTopicTest {
    @Test
    fun everyLinkOpensALessonThatExists() {
        val course = CourseLoader.bundled()
        TodayTopic.entries.forEach { topic -> assertNotNull(topic.name, course.lesson(topic.lessonId)) }
    }
}
