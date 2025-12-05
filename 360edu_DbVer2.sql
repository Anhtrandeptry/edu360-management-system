CREATE DATABASE  IF NOT EXISTS `edu360_system` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `edu360_system`;
-- MySQL dump 10.13  Distrib 8.0.42, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: edu360_system
-- ------------------------------------------------------
-- Server version	9.2.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `attendances`
--

DROP TABLE IF EXISTS `attendances`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `attendances` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `note` text,
  `status` enum('ABSENT','LATE','PRESENT','UNMARKED') NOT NULL,
  `session_id` bigint NOT NULL,
  `student_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKc8ku2g1lrpdxqk1icj91sp9is` (`session_id`,`student_id`),
  KEY `FK7bm4q4wptspkenhrsjgatdmk0` (`student_id`),
  CONSTRAINT `FK3a2tcexm2i91hhlum0bwhymb8` FOREIGN KEY (`session_id`) REFERENCES `class_sessions` (`id`),
  CONSTRAINT `FK7bm4q4wptspkenhrsjgatdmk0` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `attendances`
--

LOCK TABLES `attendances` WRITE;
/*!40000 ALTER TABLE `attendances` DISABLE KEYS */;
/*!40000 ALTER TABLE `attendances` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `class_enrollments`
--

DROP TABLE IF EXISTS `class_enrollments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `class_enrollments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `class_id` bigint NOT NULL,
  `student_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKtbsl0kmnxm5sv6tgv5elnfnnx` (`class_id`,`student_id`),
  KEY `FKmlij77rqmb1lkh5gu1ke7d5j3` (`student_id`),
  CONSTRAINT `FKh0j9fw62n3qo32qbfsq2wmcpc` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`),
  CONSTRAINT `FKmlij77rqmb1lkh5gu1ke7d5j3` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=30 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `class_enrollments`
--

LOCK TABLES `class_enrollments` WRITE;
/*!40000 ALTER TABLE `class_enrollments` DISABLE KEYS */;
/*!40000 ALTER TABLE `class_enrollments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `class_schedules`
--

DROP TABLE IF EXISTS `class_schedules`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `class_schedules` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `day_of_week` int NOT NULL,
  `class_id` bigint NOT NULL,
  `timeslot_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKd4fidccuckfhabn3u91coqo6g` (`class_id`),
  KEY `FKiyxthk4gliy5kr6ythubrrun6` (`timeslot_id`),
  CONSTRAINT `FKd4fidccuckfhabn3u91coqo6g` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`),
  CONSTRAINT `FKiyxthk4gliy5kr6ythubrrun6` FOREIGN KEY (`timeslot_id`) REFERENCES `time_slots` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=158 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `class_schedules`
--

LOCK TABLES `class_schedules` WRITE;
/*!40000 ALTER TABLE `class_schedules` DISABLE KEYS */;
INSERT INTO `class_schedules` VALUES (148,7,26,3),(149,4,26,3),(150,2,26,3),(151,1,27,3),(152,3,27,3),(153,5,27,3),(154,4,28,3),(155,7,28,3),(156,4,29,3),(157,7,29,3);
/*!40000 ALTER TABLE `class_schedules` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `class_sessions`
--

DROP TABLE IF EXISTS `class_sessions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `class_sessions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `date` date NOT NULL,
  `day_of_week` int NOT NULL,
  `lesson_content` text,
  `status` enum('CANCELLED','DONE','PLANNED') NOT NULL,
  `class_id` bigint NOT NULL,
  `room_id` bigint DEFAULT NULL,
  `timeslot_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_class_session_class_date` (`class_id`,`date`),
  KEY `FKidkmiki30thsiio6sp2bidgim` (`room_id`),
  KEY `FKq1ukbbjlhxqb1kmdh08iupqwo` (`timeslot_id`),
  CONSTRAINT `FK1w2pxpii8b2dhn7pcylfpiygh` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`),
  CONSTRAINT `FKidkmiki30thsiio6sp2bidgim` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`),
  CONSTRAINT `FKq1ukbbjlhxqb1kmdh08iupqwo` FOREIGN KEY (`timeslot_id`) REFERENCES `time_slots` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=716 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `class_sessions`
--

LOCK TABLES `class_sessions` WRITE;
/*!40000 ALTER TABLE `class_sessions` DISABLE KEYS */;
INSERT INTO `class_sessions` VALUES (656,'2025-12-07',7,NULL,'PLANNED',26,1,3),(657,'2025-12-09',2,NULL,'PLANNED',26,1,3),(658,'2025-12-11',4,NULL,'PLANNED',26,1,3),(659,'2025-12-14',7,NULL,'PLANNED',26,1,3),(660,'2025-12-16',2,NULL,'PLANNED',26,1,3),(661,'2025-12-18',4,NULL,'PLANNED',26,1,3),(662,'2025-12-21',7,NULL,'PLANNED',26,1,3),(663,'2025-12-23',2,NULL,'PLANNED',26,1,3),(664,'2025-12-25',4,NULL,'PLANNED',26,1,3),(665,'2025-12-28',7,NULL,'PLANNED',26,1,3),(666,'2025-12-19',5,NULL,'PLANNED',27,5,3),(667,'2025-12-22',1,NULL,'PLANNED',27,5,3),(668,'2025-12-24',3,NULL,'PLANNED',27,5,3),(669,'2025-12-26',5,NULL,'PLANNED',27,5,3),(670,'2025-12-29',1,NULL,'PLANNED',27,5,3),(671,'2025-12-31',3,NULL,'PLANNED',27,5,3),(672,'2026-01-02',5,NULL,'PLANNED',27,5,3),(673,'2026-01-05',1,NULL,'PLANNED',27,5,3),(674,'2026-01-07',3,NULL,'PLANNED',27,5,3),(675,'2026-01-09',5,NULL,'PLANNED',27,5,3),(676,'2026-01-12',1,NULL,'PLANNED',27,5,3),(677,'2026-01-14',3,NULL,'PLANNED',27,5,3),(678,'2026-01-16',5,NULL,'PLANNED',27,5,3),(679,'2026-01-19',1,NULL,'PLANNED',27,5,3),(680,'2026-01-21',3,NULL,'PLANNED',27,5,3),(681,'2026-01-23',5,NULL,'PLANNED',27,5,3),(682,'2026-01-26',1,NULL,'PLANNED',27,5,3),(683,'2026-01-28',3,NULL,'PLANNED',27,5,3),(684,'2026-01-30',5,NULL,'PLANNED',27,5,3),(685,'2026-02-02',1,NULL,'PLANNED',27,5,3),(686,'2026-02-04',3,NULL,'PLANNED',27,5,3),(687,'2026-02-06',5,NULL,'PLANNED',27,5,3),(688,'2026-02-09',1,NULL,'PLANNED',27,5,3),(689,'2026-02-11',3,NULL,'PLANNED',27,5,3),(690,'2026-02-13',5,NULL,'PLANNED',27,5,3),(691,'2026-02-16',1,NULL,'PLANNED',27,5,3),(692,'2026-02-18',3,NULL,'PLANNED',27,5,3),(693,'2026-02-20',5,NULL,'PLANNED',27,5,3),(694,'2026-02-23',1,NULL,'PLANNED',27,5,3),(695,'2026-02-25',3,NULL,'PLANNED',27,5,3),(696,'2026-01-11',7,NULL,'PLANNED',28,7,3),(697,'2026-01-15',4,NULL,'PLANNED',28,7,3),(698,'2026-01-18',7,NULL,'PLANNED',28,7,3),(699,'2026-01-22',4,NULL,'PLANNED',28,7,3),(700,'2026-01-25',7,NULL,'PLANNED',28,7,3),(701,'2026-01-29',4,NULL,'PLANNED',28,7,3),(702,'2026-02-01',7,NULL,'PLANNED',28,7,3),(703,'2026-02-05',4,NULL,'PLANNED',28,7,3),(704,'2026-02-08',7,NULL,'PLANNED',28,7,3),(705,'2026-02-12',4,NULL,'PLANNED',28,7,3),(706,'2026-01-01',4,NULL,'PLANNED',29,NULL,3),(707,'2026-01-04',7,NULL,'PLANNED',29,NULL,3),(708,'2026-01-08',4,NULL,'PLANNED',29,NULL,3),(709,'2026-01-11',7,NULL,'PLANNED',29,NULL,3),(710,'2026-01-15',4,NULL,'PLANNED',29,NULL,3),(711,'2026-01-18',7,NULL,'PLANNED',29,NULL,3),(712,'2026-01-22',4,NULL,'PLANNED',29,NULL,3),(713,'2026-01-25',7,NULL,'PLANNED',29,NULL,3),(714,'2026-01-29',4,NULL,'PLANNED',29,NULL,3),(715,'2026-02-01',7,NULL,'PLANNED',29,NULL,3);
/*!40000 ALTER TABLE `class_sessions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `classes`
--

DROP TABLE IF EXISTS `classes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `classes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `description` text,
  `end_date` date NOT NULL,
  `max_students` int NOT NULL,
  `meeting_link` varchar(500) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `price_per_session` bigint NOT NULL,
  `start_date` date NOT NULL,
  `status` enum('ARCHIVED','DRAFT','PUBLIC') NOT NULL,
  `course_id` bigint DEFAULT NULL,
  `room_id` bigint DEFAULT NULL,
  `semester_id` bigint DEFAULT NULL,
  `subject_id` bigint NOT NULL,
  `teacher_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK9v6ijeybapa0ontdtd4o4rycs` (`course_id`),
  KEY `FKrs15kq5uo0v2a7olcmbtuidaw` (`room_id`),
  KEY `FK8menn4iwjhqtjd5gq4bklkrj5` (`semester_id`),
  KEY `FKcaops3x4cgf4peavqc3gh87k1` (`subject_id`),
  KEY `FK8td8h5k21lq8jax2h6oobm9l0` (`teacher_id`),
  CONSTRAINT `FK8menn4iwjhqtjd5gq4bklkrj5` FOREIGN KEY (`semester_id`) REFERENCES `semesters` (`id`),
  CONSTRAINT `FK8td8h5k21lq8jax2h6oobm9l0` FOREIGN KEY (`teacher_id`) REFERENCES `teachers` (`id`),
  CONSTRAINT `FK9v6ijeybapa0ontdtd4o4rycs` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`),
  CONSTRAINT `FKcaops3x4cgf4peavqc3gh87k1` FOREIGN KEY (`subject_id`) REFERENCES `subjects` (`id`),
  CONSTRAINT `FKrs15kq5uo0v2a7olcmbtuidaw` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=30 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `classes`
--

LOCK TABLES `classes` WRITE;
/*!40000 ALTER TABLE `classes` DISABLE KEYS */;
INSERT INTO `classes` VALUES (26,'Cái tôi','2025-12-28',20,NULL,'Ngữ Văn 6 - GVTuan - V4',100000,'2025-12-06','DRAFT',40,1,NULL,5,5),(27,'Lớp đại trà','2026-02-25',40,NULL,'Ngữ Văn 6 - GVTuan - K6',50000,'2025-12-18','DRAFT',41,5,NULL,5,5),(28,'Lớp ôn cấp tốc','2026-02-12',10,NULL,'Toán 7 - GVGiang - P0',200000,'2026-01-11','DRAFT',43,7,NULL,2,6),(29,'Lớp ôn cấp tốc CÔ Nhi','2026-02-01',10,'https://meet.google.com/uba-pjgz-mww?authuser=0&pli=1','Toán 7 - GVNhi - U0',100000,'2025-12-31','DRAFT',44,NULL,NULL,2,2);
/*!40000 ALTER TABLE `classes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `course_chapters`
--

DROP TABLE IF EXISTS `course_chapters`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course_chapters` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `description` text,
  `order_index` int NOT NULL,
  `title` varchar(255) NOT NULL,
  `course_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK6e01epq13kxyl8na3qg9i48er` (`course_id`),
  CONSTRAINT `FK6e01epq13kxyl8na3qg9i48er` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=278 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `course_chapters`
--

LOCK TABLES `course_chapters` WRITE;
/*!40000 ALTER TABLE `course_chapters` DISABLE KEYS */;
INSERT INTO `course_chapters` VALUES (260,'Bài 1: Khát vọng khẳng định bản thân\n\nBài 2: Sự cô đơn trong hành trình trưởng thành',0,'Cái tôi trong thế giới rộng lớn',39),(261,'Bài 1: Văn bản 1 – Cây bút chì (trích)\n\nBài 2: Văn bản 2 – Những người bạn mới\n\nBài 3: Tìm hiểu nhân vật có cá tính nổi bật trong truyện',1,'Tiếng nói của cái tôi qua văn bản nghệ thuật',39),(262,'Bài 1: Viết đoạn văn thể hiện suy nghĩ về bản thân\n\nBài 2: Trình bày quan điểm: “Mỗi người đều là duy nhất”\n\nBài 3: Dự án nhỏ: Làm poster “Tôi là ai?”',2,'Tự tin thể hiện cái tôi',39),(263,'Bài 1: Khát vọng khẳng định bản thân\n\nBài 2: Sự cô đơn trong hành trình trưởng thành',0,'Cái tôi trong thế giới rộng lớn',40),(264,'Bài 1: Văn bản 1 – Cây bút chì (trích)\n\nBài 2: Văn bản 2 – Những người bạn mới\n\nBài 3: Tìm hiểu nhân vật có cá tính nổi bật trong truyện',1,'Tiếng nói của cái tôi qua văn bản nghệ thuật',40),(265,'Bài 1: Viết đoạn văn thể hiện suy nghĩ về bản thân\n\nBài 2: Trình bày quan điểm: “Mỗi người đều là duy nhất”\n\nBài 3: Dự án nhỏ: Làm poster “Tôi là ai?”',2,'Tự tin thể hiện cái tôi',40),(266,'Bài 1: Khát vọng khẳng định bản thân\n\nBài 2: Sự cô đơn trong hành trình trưởng thành',0,'Cái tôi trong thế giới rộng lớn',41),(267,'Bài 1: Văn bản 1 – Cây bút chì (trích)\n\nBài 2: Văn bản 2 – Những người bạn mới\n\nBài 3: Tìm hiểu nhân vật có cá tính nổi bật trong truyện',1,'Tiếng nói của cái tôi qua văn bản nghệ thuật',41),(268,'Bài 1: Viết đoạn văn thể hiện suy nghĩ về bản thân\n\nBài 2: Trình bày quan điểm: “Mỗi người đều là duy nhất”\n\nBài 3: Dự án nhỏ: Làm poster “Tôi là ai?”',2,'Tự tin thể hiện cái tôi',41),(269,'Bài 1: Biểu thức và hằng đẳng thức – Bài tập chọn lọc\n\nBài 2: Tính chất chia hết – Bài toán nâng cao\n\nBài 3: Số nguyên và bài toán thực tế khó\n\nBài 4: Dạng đề tổng hợp Số học (Có chữa đề mẫu)',0,'Số học nâng cao',42),(270,'Bài 1: Giải bài toán bằng lập phương trình\n\nBài 2: Dãy số, quy luật số học\n\nBài 3: Toán tư duy & Bài toán nâng cao\n\nBài 4: Chữa đề thi Đại số năm 2022–2023',1,'Đại số và biểu thức bậc nhất',42),(271,'Bài 1: Quan hệ đường thẳng – góc trong tam giác\n\nBài 2: Tam giác đồng dạng & các bài toán biến đổi\n\nBài 3: Hình học dựng và chứng minh nâng cao\n\nBài 4: Chữa đề hình học năm 2022–2023',2,'Hình học nâng cao',42),(272,'Bài 1: Biểu thức và hằng đẳng thức – Bài tập chọn lọc\n\nBài 2: Tính chất chia hết – Bài toán nâng cao\n\nBài 3: Số nguyên và bài toán thực tế khó\n\nBài 4: Dạng đề tổng hợp Số học (Có chữa đề mẫu)',0,'Số học nâng cao',43),(273,'Bài 1: Giải bài toán bằng lập phương trình\n\nBài 2: Dãy số, quy luật số học\n\nBài 3: Toán tư duy & Bài toán nâng cao\n\nBài 4: Chữa đề thi Đại số năm 2022–2023',1,'Đại số và biểu thức bậc nhất',43),(274,'Bài 1: Quan hệ đường thẳng – góc trong tam giác\n\nBài 2: Tam giác đồng dạng & các bài toán biến đổi\n\nBài 3: Hình học dựng và chứng minh nâng cao\n\nBài 4: Chữa đề hình học năm 2022–2023',2,'Hình học nâng cao',43),(275,'Bài 1: Biểu thức và hằng đẳng thức – Bài tập chọn lọc\n\nBài 2: Tính chất chia hết – Bài toán nâng cao\n\nBài 3: Số nguyên và bài toán thực tế khó\n\nBài 4: Dạng đề tổng hợp Số học (Có chữa đề mẫu)',0,'Số học nâng cao',44),(276,'Bài 1: Giải bài toán bằng lập phương trình\n\nBài 2: Dãy số, quy luật số học\n\nBài 3: Toán tư duy & Bài toán nâng cao\n\nBài 4: Chữa đề thi Đại số năm 2022–2023',1,'Đại số và biểu thức bậc nhất',44),(277,'Bài 1: Quan hệ đường thẳng – góc trong tam giác\n\nBài 2: Tam giác đồng dạng & các bài toán biến đổi\n\nBài 3: Hình học dựng và chứng minh nâng cao\n\nBài 4: Chữa đề hình học năm 2022–2023',2,'Hình học nâng cao',44);
/*!40000 ALTER TABLE `course_chapters` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `course_lessons`
--

DROP TABLE IF EXISTS `course_lessons`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course_lessons` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `description` text,
  `order_index` int NOT NULL,
  `title` varchar(255) NOT NULL,
  `chapter_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKj9348iexqy0mt72i70uhgr64x` (`chapter_id`),
  CONSTRAINT `FKj9348iexqy0mt72i70uhgr64x` FOREIGN KEY (`chapter_id`) REFERENCES `course_chapters` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=993 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `course_lessons`
--

LOCK TABLES `course_lessons` WRITE;
/*!40000 ALTER TABLE `course_lessons` DISABLE KEYS */;
INSERT INTO `course_lessons` VALUES (933,'',0,'Bài 1: Khát vọng khẳng định bản thân',260),(934,'',1,'Bài 2: Sự cô đơn trong hành trình trưởng thành',260),(935,'',0,'Bài 1: Văn bản 1 – Cây bút chì (trích)',261),(936,'',1,'Bài 2: Văn bản 2 – Những người bạn mới',261),(937,'',2,'Bài 3: Tìm hiểu nhân vật có cá tính nổi bật trong truyện',261),(938,'',0,'Bài 1: Viết đoạn văn thể hiện suy nghĩ về bản thân',262),(939,'',1,'Bài 2: Trình bày quan điểm: “Mỗi người đều là duy nhất”',262),(940,'',2,'Bài 3: Dự án nhỏ: Làm poster “Tôi là ai?”',262),(941,'',0,'Bài 1: Khát vọng khẳng định bản thân',263),(942,'',1,'Bài 2: Sự cô đơn trong hành trình trưởng thành',263),(943,'',0,'Bài 1: Văn bản 1 – Cây bút chì (trích)',264),(944,'',1,'Bài 2: Văn bản 2 – Những người bạn mới',264),(945,'',2,'Bài 3: Tìm hiểu nhân vật có cá tính nổi bật trong truyện',264),(946,'',0,'Bài 1: Viết đoạn văn thể hiện suy nghĩ về bản thân',265),(947,'',1,'Bài 2: Trình bày quan điểm: “Mỗi người đều là duy nhất”',265),(948,'',2,'Bài 3: Dự án nhỏ: Làm poster “Tôi là ai?”',265),(949,'',0,'Bài 1: Khát vọng khẳng định bản thân',266),(950,'',1,'Bài 2: Sự cô đơn trong hành trình trưởng thành',266),(951,'',0,'Bài 1: Văn bản 1 – Cây bút chì (trích)',267),(952,'',1,'Bài 2: Văn bản 2 – Những người bạn mới',267),(953,'',2,'Bài 3: Tìm hiểu nhân vật có cá tính nổi bật trong truyện',267),(954,'',0,'Bài 1: Viết đoạn văn thể hiện suy nghĩ về bản thân',268),(955,'',1,'Bài 2: Trình bày quan điểm: “Mỗi người đều là duy nhất”',268),(956,'',2,'Bài 3: Dự án nhỏ: Làm poster “Tôi là ai?”',268),(957,'',0,'Bài 1: Biểu thức và hằng đẳng thức – Bài tập chọn lọc',269),(958,'',1,'Bài 2: Tính chất chia hết – Bài toán nâng cao',269),(959,'',2,'Bài 3: Số nguyên và bài toán thực tế khó',269),(960,'',3,'Bài 4: Dạng đề tổng hợp Số học (Có chữa đề mẫu)',269),(961,'',0,'Bài 1: Giải bài toán bằng lập phương trình',270),(962,'',1,'Bài 2: Dãy số, quy luật số học',270),(963,'',2,'Bài 3: Toán tư duy & Bài toán nâng cao',270),(964,'',3,'Bài 4: Chữa đề thi Đại số năm 2022–2023',270),(965,'',0,'Bài 1: Quan hệ đường thẳng – góc trong tam giác',271),(966,'',1,'Bài 2: Tam giác đồng dạng & các bài toán biến đổi',271),(967,'',2,'Bài 3: Hình học dựng và chứng minh nâng cao',271),(968,'',3,'Bài 4: Chữa đề hình học năm 2022–2023',271),(969,'',0,'Bài 1: Biểu thức và hằng đẳng thức – Bài tập chọn lọc',272),(970,'',1,'Bài 2: Tính chất chia hết – Bài toán nâng cao',272),(971,'',2,'Bài 3: Số nguyên và bài toán thực tế khó',272),(972,'',3,'Bài 4: Dạng đề tổng hợp Số học (Có chữa đề mẫu)',272),(973,'',0,'Bài 1: Giải bài toán bằng lập phương trình',273),(974,'',1,'Bài 2: Dãy số, quy luật số học',273),(975,'',2,'Bài 3: Toán tư duy & Bài toán nâng cao',273),(976,'',3,'Bài 4: Chữa đề thi Đại số năm 2022–2023',273),(977,'',0,'Bài 1: Quan hệ đường thẳng – góc trong tam giác',274),(978,'',1,'Bài 2: Tam giác đồng dạng & các bài toán biến đổi',274),(979,'',2,'Bài 3: Hình học dựng và chứng minh nâng cao',274),(980,'',3,'Bài 4: Chữa đề hình học năm 2022–2023',274),(981,'',0,'Bài 1: Biểu thức và hằng đẳng thức – Bài tập chọn lọc',275),(982,'',1,'Bài 2: Tính chất chia hết – Bài toán nâng cao',275),(983,'',2,'Bài 3: Số nguyên và bài toán thực tế khó',275),(984,'',3,'Bài 4: Dạng đề tổng hợp Số học (Có chữa đề mẫu)',275),(985,'',0,'Bài 1: Giải bài toán bằng lập phương trình',276),(986,'',1,'Bài 2: Dãy số, quy luật số học',276),(987,'',2,'Bài 3: Toán tư duy & Bài toán nâng cao',276),(988,'',3,'Bài 4: Chữa đề thi Đại số năm 2022–2023',276),(989,'',0,'Bài 1: Quan hệ đường thẳng – góc trong tam giác',277),(990,'',1,'Bài 2: Tam giác đồng dạng & các bài toán biến đổi',277),(991,'',2,'Bài 3: Hình học dựng và chứng minh nâng cao',277),(992,'',3,'Bài 4: Chữa đề hình học năm 2022–2023',277);
/*!40000 ALTER TABLE `course_lessons` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `courses`
--

DROP TABLE IF EXISTS `courses`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `courses` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `description` text,
  `status` enum('APPROVED','ARCHIVED','DRAFT','PENDING','REJECTED') NOT NULL,
  `title` varchar(255) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `created_by_user_id` bigint NOT NULL,
  `teacher_id` bigint DEFAULT NULL,
  `subject_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKabpqukrjpy93jgv6v376v1mif` (`created_by_user_id`),
  KEY `FK468oyt88pgk2a0cxrvxygadqg` (`teacher_id`),
  KEY `FK5tckdihu5akp5nkxiacx1gfhi` (`subject_id`),
  CONSTRAINT `FK468oyt88pgk2a0cxrvxygadqg` FOREIGN KEY (`teacher_id`) REFERENCES `teachers` (`id`),
  CONSTRAINT `FK5tckdihu5akp5nkxiacx1gfhi` FOREIGN KEY (`subject_id`) REFERENCES `subjects` (`id`),
  CONSTRAINT `FKabpqukrjpy93jgv6v376v1mif` FOREIGN KEY (`created_by_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=45 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `courses`
--

LOCK TABLES `courses` WRITE;
/*!40000 ALTER TABLE `courses` DISABLE KEYS */;
INSERT INTO `courses` VALUES (39,'2025-12-05 00:23:29.630147','Khóa học “Cô đơn làm nên cái tôi” giúp học sinh khám phá giá trị của sự khác biệt, hiểu và trân trọng bản sắc cá nhân.\nThông qua việc đọc hiểu văn bản thuộc nhiều thể loại và thực hành viết – nói – nghe, học sinh hình thành khả năng tự nhận thức, diễn đạt suy nghĩ và cảm xúc của mình một cách tự tin, sáng tạo và nhân văn.\n? Mục tiêu:\n\nBồi dưỡng năng lực cảm thụ văn học và tư duy phản biện\n\nRèn kĩ năng giao tiếp và biểu đạt quan điểm cá nhân\n\nTôn trọng sự khác biệt và giá trị riêng của mỗi con người','APPROVED','Ngữ Văn 6 - Cô đơn làm nên cái tôi','2025-12-05 00:23:29.630147',2,NULL,5),(40,'2025-12-05 00:24:19.522310','Khóa học “Cô đơn làm nên cái tôi” giúp học sinh khám phá giá trị của sự khác biệt, hiểu và trân trọng bản sắc cá nhân.\nThông qua việc đọc hiểu văn bản thuộc nhiều thể loại và thực hành viết – nói – nghe, học sinh hình thành khả năng tự nhận thức, diễn đạt suy nghĩ và cảm xúc của mình một cách tự tin, sáng tạo và nhân văn.\n? Mục tiêu:\n\nBồi dưỡng năng lực cảm thụ văn học và tư duy phản biện\n\nRèn kĩ năng giao tiếp và biểu đạt quan điểm cá nhân\n\nTôn trọng sự khác biệt và giá trị riêng của mỗi con người\n[[SOURCE:39]]','APPROVED','Ngữ Văn 6 - Cô đơn làm nên cái tôi - Ngữ Văn 6 - GVTuan - R3','2025-12-05 00:24:19.522310',11,5,5),(41,'2025-12-05 00:25:11.898918','Khóa học “Cô đơn làm nên cái tôi” giúp học sinh khám phá giá trị của sự khác biệt, hiểu và trân trọng bản sắc cá nhân.\nThông qua việc đọc hiểu văn bản thuộc nhiều thể loại và thực hành viết – nói – nghe, học sinh hình thành khả năng tự nhận thức, diễn đạt suy nghĩ và cảm xúc của mình một cách tự tin, sáng tạo và nhân văn.\n? Mục tiêu:\n\nBồi dưỡng năng lực cảm thụ văn học và tư duy phản biện\n\nRèn kĩ năng giao tiếp và biểu đạt quan điểm cá nhân\n\nTôn trọng sự khác biệt và giá trị riêng của mỗi con người\n[[SOURCE:39]]','APPROVED','Ngữ Văn 6 - Cô đơn làm nên cái tôi - Ngữ Văn 6 - GVTuan - K6','2025-12-05 00:25:11.898918',11,5,5),(42,'2025-12-05 00:27:59.331357','Khóa học “Toán 7 – Chữa đề thi cấp tỉnh khóa 2022–2023” tập trung giúp học sinh làm quen với cấu trúc đề thi, rèn luyện tư duy và kỹ năng giải toán theo chuẩn đánh giá năng lực tại các kỳ thi học sinh giỏi cấp tỉnh.\n\nTrong mỗi bài học, học sinh được phân tích lỗi sai thường gặp, hướng dẫn phương pháp giải nhanh – logic – tối ưu thời gian, từ đó tự tin hơn trong quá trình ôn luyện.\n\n? Mục tiêu khóa học:\n\nNắm vững các chuyên đề trọng tâm trọng điểm của kỳ thi\n\nRèn luyện kỹ năng xử lý bài khó, nâng cao khả năng suy luận\n\nTự tin trước các bài toán vận dụng & vận dụng cao\n\nPhát triển tư duy giải toán sáng tạo và khoa học\n\n? Đối tượng: Học sinh lớp 7 có định hướng tham gia kỳ thi học sinh giỏi cấp huyện/tỉnh','APPROVED','Toán học 7 - Chữa đề thi cấp tỉnh khóa 2022-2023','2025-12-05 00:27:59.331357',2,NULL,2),(43,'2025-12-05 00:28:32.427885','Khóa học “Toán 7 – Chữa đề thi cấp tỉnh khóa 2022–2023” tập trung giúp học sinh làm quen với cấu trúc đề thi, rèn luyện tư duy và kỹ năng giải toán theo chuẩn đánh giá năng lực tại các kỳ thi học sinh giỏi cấp tỉnh.\n\nTrong mỗi bài học, học sinh được phân tích lỗi sai thường gặp, hướng dẫn phương pháp giải nhanh – logic – tối ưu thời gian, từ đó tự tin hơn trong quá trình ôn luyện.\n\n? Mục tiêu khóa học:\n\nNắm vững các chuyên đề trọng tâm trọng điểm của kỳ thi\n\nRèn luyện kỹ năng xử lý bài khó, nâng cao khả năng suy luận\n\nTự tin trước các bài toán vận dụng & vận dụng cao\n\nPhát triển tư duy giải toán sáng tạo và khoa học\n\n? Đối tượng: Học sinh lớp 7 có định hướng tham gia kỳ thi học sinh giỏi cấp huyện/tỉnh\n[[SOURCE:42]]','APPROVED','Toán học 7 - Chữa đề thi cấp tỉnh khóa 2022-2023 - Toán 7 - GVGiang - P0','2025-12-05 00:28:32.427885',12,6,2),(44,'2025-12-05 00:29:23.103331','Khóa học “Toán 7 – Chữa đề thi cấp tỉnh khóa 2022–2023” tập trung giúp học sinh làm quen với cấu trúc đề thi, rèn luyện tư duy và kỹ năng giải toán theo chuẩn đánh giá năng lực tại các kỳ thi học sinh giỏi cấp tỉnh.\n\nTrong mỗi bài học, học sinh được phân tích lỗi sai thường gặp, hướng dẫn phương pháp giải nhanh – logic – tối ưu thời gian, từ đó tự tin hơn trong quá trình ôn luyện.\n\n? Mục tiêu khóa học:\n\nNắm vững các chuyên đề trọng tâm trọng điểm của kỳ thi\n\nRèn luyện kỹ năng xử lý bài khó, nâng cao khả năng suy luận\n\nTự tin trước các bài toán vận dụng & vận dụng cao\n\nPhát triển tư duy giải toán sáng tạo và khoa học\n\n? Đối tượng: Học sinh lớp 7 có định hướng tham gia kỳ thi học sinh giỏi cấp huyện/tỉnh\n[[SOURCE:42]]','APPROVED','Toán học 7 - Chữa đề thi cấp tỉnh khóa 2022-2023 - Toán 7 - GVNhi - U0','2025-12-05 00:29:23.103331',4,2,2);
/*!40000 ALTER TABLE `courses` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `lesson_materials`
--

DROP TABLE IF EXISTS `lesson_materials`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lesson_materials` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `description` varchar(500) DEFAULT NULL,
  `file_name` varchar(255) NOT NULL,
  `file_size` bigint DEFAULT NULL,
  `file_type` varchar(100) DEFAULT NULL,
  `file_url` varchar(1000) NOT NULL,
  `uploaded_at` datetime(6) NOT NULL,
  `lesson_id` bigint NOT NULL,
  `uploaded_by` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_lesson_material_lesson` (`lesson_id`),
  KEY `FKa3kmb7ku2ui2k0lnfr1e1y689` (`uploaded_by`),
  CONSTRAINT `FKa3kmb7ku2ui2k0lnfr1e1y689` FOREIGN KEY (`uploaded_by`) REFERENCES `users` (`id`),
  CONSTRAINT `FKmdbeuu5vjwny92do4vh317tqq` FOREIGN KEY (`lesson_id`) REFERENCES `course_lessons` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `lesson_materials`
--

LOCK TABLES `lesson_materials` WRITE;
/*!40000 ALTER TABLE `lesson_materials` DISABLE KEYS */;
/*!40000 ALTER TABLE `lesson_materials` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `news`
--

DROP TABLE IF EXISTS `news`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `news` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `author` varchar(100) DEFAULT NULL,
  `content` text NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `excerpt` text,
  `image_url` longtext,
  `published_at` datetime(6) DEFAULT NULL,
  `status` varchar(20) NOT NULL,
  `tags` varchar(500) DEFAULT NULL,
  `title` varchar(500) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `views` int NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `news`
--

LOCK TABLES `news` WRITE;
/*!40000 ALTER TABLE `news` DISABLE KEYS */;
INSERT INTO `news` VALUES (1,'Admin','360edu PRO chính thức công bố sự hợp tác chiến lược với Thầy Nguyễn Ngọc Hùng, Thạc sĩ Toán Ứng dụng, một trong những giảng viên được yêu thích nhất tại Trường Đại học FPT Đà Nẵng.\n\n\n\nChuyên môn và Kinh nghiệm: Với hơn 10 năm kinh nghiệm giảng dạy, Thầy Hùng không chỉ truyền đạt kiến thức Toán học (từ nền tảng THCS đến Toán Cao cấp) mà còn chú trọng phát triển tư duy logic và kỹ năng giải quyết vấn đề cho học sinh. Thành tích nổi bật của Thầy là giúp hàng trăm học sinh đạt điểm cao trong các kỳ thi học sinh giỏi và đỗ vào các trường đại học top đầu.\n\n\n\nLớp học Phụ trách: Thầy Hùng sẽ trực tiếp phụ trách các khóa Toán 6 Đặc biệt Cấp tốc và các lớp ôn luyện chuyên sâu cho học sinh THCS/THPT. Phương pháp giảng dạy của Thầy đảm bảo dễ hiểu, trực quan, giúp học sinh mất gốc lấy lại nền tảng nhanh chóng.\n\n\n\n****\n\n\n\nQuý phụ huynh và học viên có thể đăng ký ngay các khóa học của Thầy Nguyễn Ngọc Hùng để được học tập trong môi trường chuyên nghiệp, chất lượng cao!','2025-12-01 14:32:43.726806','Chào đón Thầy Nguyễn Ngọc Hùng, chuyên gia Toán học với 10 năm kinh nghiệm từ ĐH FPT Đà Nẵng, sẽ trực tiếp phụ trách các lớp Toán 6-12 và ôn thi chuyên tại Trung tâm 360edu PRO.','https://sf-static.upanhlaylink.com/img/image_202512011e50286e181ba26fc94fb69e49367d91.jpg','2025-12-01 14:32:43.715425','PUBLISHED','','[HOT] Giảng viên Thạc sĩ Nguyễn Ngọc Hùng (ĐH FPT) Chính thức gia nhập đội ngũ 360edu','2025-12-01 14:32:43.726806',0),(2,'Admin','Kính gửi Quý phụ huynh và các em học sinh thân mến,\n\n\n\nTrong không khí hân hoan của những ngày đầu năm học mới, Trung tâm 360edu PRO vui mừng thông báo Lễ Khai Giảng chính thức các khóa học [Kỳ học] đã diễn ra thành công tốt đẹp!\n\n\n\nSự kiện đã chào đón hàng trăm học viên mới cùng toàn thể đội ngũ giáo viên giàu kinh nghiệm, trong đó có sự góp mặt của Thạc sĩ Nguyễn Ngọc Hùng (Toán) và Cô Dương Uyển Nhi (Văn), hứa hẹn một năm học bùng nổ và đạt nhiều thành tích cao.\n\n\n\nTại 360edu PRO, chúng tôi cam kết mang đến môi trường học tập hiện đại, cá nhân hóa, giúp học viên không chỉ giỏi kiến thức mà còn phát triển tư duy toàn diện. Tất cả các lớp học đã bắt đầu đi vào ổn định theo đúng lịch trình.\n\n\n\n****\n\n\n\nChúc toàn thể học viên của 360edu có một năm học thật nhiều niềm vui, khám phá và chinh phục được mọi mục tiêu học tập của mình!','2025-12-01 14:34:24.962823','Trung tâm 360edu (e360edu PRO) chính thức khai giảng năm học [Năm hiện tại], chào đón hàng trăm học viên cùng đội ngũ giáo viên chất lượng cao!','https://sf-static.upanhlaylink.com/img/image_20251201b120b422b6e4f3e58cb5f34a90a7b589.jpg','2025-12-01 14:34:24.961819','PUBLISHED','','CHÍNH THỨC KHAI GIẢNG NĂM HỌC MỚI: CÙNG 360EDU CHINH PHỤC TRI THỨC!','2025-12-01 14:34:24.962823',0),(3,'Admin','360edu PRO chính thức giới thiệu Cô Dương Uyển Nhi, Cử nhân Ngữ Văn và là một trong những giáo viên được học sinh yêu thích nhất tại khu vực.\n\n\n\nChuyên môn và Kinh nghiệm: Với 5 năm kinh nghiệm chuyên sâu trong việc truyền cảm hứng Văn học cho học sinh THCS, Cô Nhi đặc biệt giỏi trong việc phân tích các tác phẩm tự sự dân gian (lớp 6) và rèn luyện kỹ năng viết Nghị luận Xã hội. Phương pháp của Cô Nhi không chỉ tập trung vào điểm số mà còn giúp học sinh phát triển tư duy phản biện và khả năng cảm thụ nghệ thuật.\n\n\n\nLớp học Phụ trách: Cô Uyển Nhi sẽ trực tiếp phụ trách các khóa Ngữ Văn 6 - Khám phá Văn học Dân gian và các lớp ôn luyện chuyên đề cho học sinh khối 8, 9. Sĩ số lớp học được giữ ở mức tối ưu để đảm bảo chất lượng tương tác và kèm cặp từng học sinh.\n\n\n\n****\n\n\n\nQuý phụ huynh và học viên hãy đăng ký ngay các khóa Văn học của Cô Dương Uyển Nhi để giúp con bạn biến môn Văn thành niềm yêu thích và đạt kết quả cao nhất!','2025-12-01 14:37:00.031404','Trung tâm 360edu (e360edu PRO) vui mừng thông báo sự gia nhập của Cô Dương Uyển Nhi, Cử nhân Văn học, mang đến phương pháp giảng dạy hiện đại, giúp học sinh yêu thích và giỏi môn Văn.','https://sf-static.upanhlaylink.com/img/image_202512014fa21775e03275b7cc8f1dfb3160b11b.jpg','2025-12-01 14:37:00.030120','PUBLISHED','','CHÀO ĐÓN CÔ DƯƠNG UYỂN NHI: Chuyên gia Khơi nguồn Cảm hứng Văn học gia nhập 360edu','2025-12-01 14:37:00.031404',0);
/*!40000 ALTER TABLE `news` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notifications`
--

DROP TABLE IF EXISTS `notifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notifications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `is_read` bit(1) NOT NULL,
  `link` varchar(255) DEFAULT NULL,
  `message` text,
  `read_at` datetime(6) DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `type` varchar(50) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK9y21adhxn0ayjhfocscqox7bh` (`user_id`),
  CONSTRAINT `FK9y21adhxn0ayjhfocscqox7bh` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=26 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notifications`
--

LOCK TABLES `notifications` WRITE;
/*!40000 ALTER TABLE `notifications` DISABLE KEYS */;
INSERT INTO `notifications` VALUES (5,'2025-12-02 18:52:02.216439',_binary '','/home/my-classes','Bạn đã thanh toán 60.000đ cho lớp Nguyễn Nam Anh Tuấn - Hóa Học','2025-12-02 18:53:08.720334','Thanh toán thành công','PAYMENT_SUCCESS',9),(6,'2025-12-02 18:52:02.221545',_binary '','/home/my-classes/12','Bạn đã được thêm vào lớp: Nguyễn Nam Anh Tuấn - Hóa Học','2025-12-02 18:53:08.720334','Đăng ký lớp thành công','ENROLLED_NEW_CLASS',9),(7,'2025-12-04 02:14:07.269042',_binary '','/home/my-classes','Bạn đã thanh toán 2.400.000đ cho lớp Toán - GVHung - H4','2025-12-04 02:31:35.997477','Thanh toán thành công','PAYMENT_SUCCESS',7),(8,'2025-12-04 02:14:07.276068',_binary '','/home/my-classes/23','Bạn đã được thêm vào lớp: Toán - GVHung - H4','2025-12-04 02:31:35.997477','Đăng ký lớp thành công','ENROLLED_NEW_CLASS',7),(9,'2025-12-04 03:47:31.516655',_binary '','/home/student/schedule','Bạn được điểm danh CÓ MẶT tại lớp Toán - GVHung - H4, Tiết 2 (18:00:00 - 20:00:00) ngày 04/12/2025.\n? Ghi chú từ giáo viên: đi muộn 15p','2025-12-04 03:48:28.424823','✅ Điểm danh: Có mặt','CLASS_REMINDER',7),(10,'2025-12-04 03:47:44.528954',_binary '','/home/student/schedule','Bạn được điểm danh VẮNG MẶT tại lớp Toán - GVHung - H4, Tiết 2 (18:00:00 - 20:00:00) ngày 04/12/2025.\n? Ghi chú từ giáo viên: ko đi học','2025-12-04 03:48:11.811273','❌ Điểm danh: Vắng mặt','CLASS_REMINDER',7),(11,'2025-12-04 04:03:11.840759',_binary '','/home/student/schedule','Bạn được điểm danh ĐI MUỘN tại lớp Toán - GVHung - H4, Tiết 2 (18:00:00 - 20:00:00) ngày 04/12/2025.\n? Ghi chú từ giáo viên: em đi muộn 10p','2025-12-04 04:04:25.127319','⚠️ Điểm danh: Đi muộn','CLASS_REMINDER',7),(12,'2025-12-04 04:04:01.154708',_binary '','/home/student/schedule','Giáo viên Nguyễn Ngọc Hùng đã đăng link tài liệu cho lớp Toán - GVHung - H4, Tiết 2 (18:00:00 - 20:00:00) ngày 04/12/2025.\n? https://www.facebook.com/ighoorbeos','2025-12-04 04:04:25.127319','? Link tài liệu mới','NEW_LESSON_AVAILABLE',7),(13,'2025-12-04 04:25:15.527460',_binary '','/home/student/courses/37','Giáo viên Nguyễn Ngọc Hùng đã thêm link cho bài học \"Biến đổi tương đương & phương trình vô tỉ\" (Chương: Phương trình & Hệ phương trình nâng cao) trong khóa học Toán 9 - Bồi dưỡng học sinh giỏi - Toán - GVHung - R6.\n? https://drive.google.com/drive/folders/1L_nz5HISdJ_wy2AVBPvSOm-BwgzZ0pN2?usp=drive_link','2025-12-04 04:26:17.265535','? Link tài liệu mới','NEW_LESSON_AVAILABLE',7),(14,'2025-12-04 04:26:50.452974',_binary '','/home/student/courses/37','Giáo viên Nguyễn Ngọc Hùng đã thêm link cho bài học \"Biến đổi tương đương & phương trình vô tỉ\" (Chương: Phương trình & Hệ phương trình nâng cao) trong khóa học Toán 9 - Bồi dưỡng học sinh giỏi - Toán - GVHung - R6.\n? https://drive.google.com/drive/folders/1L_nz5HISdJ_wy2AVBPvSOm-BwgzZ0pN2?usp=drive_link','2025-12-04 04:27:13.230701','? Link tài liệu mới','NEW_LESSON_AVAILABLE',7),(15,'2025-12-04 04:26:55.060132',_binary '','/home/student/courses/37','Giáo viên Nguyễn Ngọc Hùng đã thêm link cho bài học \"Biến đổi tương đương & phương trình vô tỉ\" (Chương: Phương trình & Hệ phương trình nâng cao) trong khóa học Toán 9 - Bồi dưỡng học sinh giỏi - Toán - GVHung - R6.\n? https://drive.google.com/drive/folders/1L_nz5HISdJ_wy2AVBPvSOm-BwgzZ0pN2?usp=drive_link','2025-12-04 04:27:13.230701','? Link tài liệu mới','NEW_LESSON_AVAILABLE',7),(16,'2025-12-04 04:26:56.874096',_binary '','/home/student/courses/37','Giáo viên Nguyễn Ngọc Hùng đã thêm link cho bài học \"Biến đổi tương đương & phương trình vô tỉ\" (Chương: Phương trình & Hệ phương trình nâng cao) trong khóa học Toán 9 - Bồi dưỡng học sinh giỏi - Toán - GVHung - R6.\n? https://drive.google.com/drive/folders/1L_nz5HISdJ_wy2AVBPvSOm-BwgzZ0pN2?usp=drive_link','2025-12-04 04:27:13.230701','? Link tài liệu mới','NEW_LESSON_AVAILABLE',7),(17,'2025-12-04 04:28:25.953282',_binary '','/home/student/courses/37','Giáo viên Nguyễn Ngọc Hùng đã thêm link cho bài học \"Biến đổi tương đương & phương trình vô tỉ\" (Chương: Phương trình & Hệ phương trình nâng cao) trong khóa học Toán 9 - Bồi dưỡng học sinh giỏi - Toán - GVHung - R6.\n? https://drive.google.com/drive/folders/1L_nz5HISdJ_wy2AVBPvSOm-BwgzZ0pN2?usp=drive_link','2025-12-04 04:28:39.535015','? Link tài liệu mới','NEW_LESSON_AVAILABLE',7),(18,'2025-12-04 04:33:57.663686',_binary '','/home/student/courses/37','Giáo viên Nguyễn Ngọc Hùng đã thêm link cho bài học \"Bài toán năng suất – chuyển động – tỉ lệ\" (Chương: Bài toán thực tế & mô hình hóa) trong khóa học Toán 9 - Bồi dưỡng học sinh giỏi - Toán - GVHung - R6.\n? https://drive.google.com/drive/folders/1L_nz5HISdJ_wy2AVBPvSOm-BwgzZ0pN2?usp=drive_link','2025-12-04 08:23:12.738131','? Link tài liệu mới','NEW_LESSON_AVAILABLE',7),(19,'2025-12-04 08:21:15.303517',_binary '','/home/student/schedule','Bạn được điểm danh ĐI MUỘN tại lớp Toán - GVHung - H4, Tiết 1 (16:00:00 - 18:00:00) ngày 04/12/2025.','2025-12-04 08:23:12.738131','⚠️ Điểm danh: Đi muộn','CLASS_REMINDER',7),(20,'2025-12-04 08:21:32.761780',_binary '','/home/student/schedule','Giáo viên Nguyễn Ngọc Hùng đã đăng link tài liệu cho lớp Toán - GVHung - H4, Tiết 1 (16:00:00 - 18:00:00) ngày 04/12/2025.\n? http://localhost:8386/home/teacher/class/23?slotId=1&date=2025-12-04','2025-12-04 08:23:12.738131','? Link tài liệu mới','NEW_LESSON_AVAILABLE',7),(21,'2025-12-04 09:02:52.265338',_binary '','/home/my-classes','Bạn đã thanh toán 2.000.000đ cho lớp Hóa Học - GVTuan - L3','2025-12-04 09:39:16.874597','Thanh toán thành công','PAYMENT_SUCCESS',9),(22,'2025-12-04 09:02:52.270350',_binary '','/home/my-classes/20','Bạn đã được thêm vào lớp: Hóa Học - GVTuan - L3','2025-12-04 09:39:16.874597','Đăng ký lớp thành công','ENROLLED_NEW_CLASS',9),(23,'2025-12-04 10:03:08.765504',_binary '','/home/my-classes','Bạn đã thanh toán 2.400.000đ cho lớp Toán - GVHung - H4','2025-12-04 10:20:28.915524','Thanh toán thành công','PAYMENT_SUCCESS',9),(24,'2025-12-04 10:03:08.775528',_binary '','/home/my-classes/23','Bạn đã được thêm vào lớp: Toán - GVHung - H4','2025-12-04 10:20:28.915524','Đăng ký lớp thành công','ENROLLED_NEW_CLASS',9),(25,'2025-12-04 10:16:04.514896',_binary '','/home/student/schedule','Bạn được điểm danh VẮNG MẶT tại lớp Toán - GVHung - H4, Tiết 1 (16:00:00 - 18:00:00) ngày 04/12/2025.','2025-12-04 10:20:28.915524','❌ Điểm danh: Vắng mặt','CLASS_REMINDER',9);
/*!40000 ALTER TABLE `notifications` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `parent_email_notifications`
--

DROP TABLE IF EXISTS `parent_email_notifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `parent_email_notifications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `email_content` text,
  `notification_date` date NOT NULL,
  `parent_email` varchar(255) NOT NULL,
  `sent_at` datetime(6) NOT NULL,
  `session_count` int DEFAULT NULL,
  `student_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK3tw7hfeyw1saw1u68otl3wp5x` (`student_id`,`notification_date`),
  KEY `idx_pen_student_date` (`student_id`,`notification_date`),
  KEY `idx_pen_sent_at` (`sent_at`),
  CONSTRAINT `FKbg1it2eiyy2cyl4o61v7q0cs6` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `parent_email_notifications`
--

LOCK TABLES `parent_email_notifications` WRITE;
/*!40000 ALTER TABLE `parent_email_notifications` DISABLE KEYS */;
/*!40000 ALTER TABLE `parent_email_notifications` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `parents`
--

DROP TABLE IF EXISTS `parents`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `parents` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `address` varchar(255) DEFAULT NULL,
  `occupation` varchar(255) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKc1t2v6wf187l8w0yew9sph3l4` (`user_id`),
  CONSTRAINT `FKchh8tf8w072tapgqoijrahojk` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `parents`
--

LOCK TABLES `parents` WRITE;
/*!40000 ALTER TABLE `parents` DISABLE KEYS */;
INSERT INTO `parents` VALUES (1,NULL,NULL,1),(2,NULL,NULL,6),(3,NULL,NULL,8);
/*!40000 ALTER TABLE `parents` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `payments`
--

DROP TABLE IF EXISTS `payments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `amount` bigint NOT NULL,
  `bank_transaction_id` varchar(255) DEFAULT NULL,
  `content` varchar(255) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `order_code` varchar(100) NOT NULL,
  `paid_at` datetime(6) DEFAULT NULL,
  `status` enum('FAILED','PAID','PENDING') NOT NULL,
  `class_id` bigint NOT NULL,
  `student_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKovxlogodb89gevtemunxvm7qt` (`class_id`,`student_id`),
  UNIQUE KEY `UKkmphhe7v8xfunkcpcylydx0xy` (`order_code`),
  KEY `FK6ooq278k2bs5xi8t5o6oort1v` (`student_id`),
  CONSTRAINT `FK6ooq278k2bs5xi8t5o6oort1v` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`),
  CONSTRAINT `FKl5ql5neuwo4yaweyppsmj14ll` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `payments`
--

LOCK TABLES `payments` WRITE;
/*!40000 ALTER TABLE `payments` DISABLE KEYS */;
/*!40000 ALTER TABLE `payments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `roles`
--

DROP TABLE IF EXISTS `roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `roles` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` enum('ROLE_ADMIN','ROLE_PARENT','ROLE_STUDENT','ROLE_TEACHER') DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `roles`
--

LOCK TABLES `roles` WRITE;
/*!40000 ALTER TABLE `roles` DISABLE KEYS */;
INSERT INTO `roles` VALUES (1,'ROLE_ADMIN'),(2,'ROLE_TEACHER'),(3,'ROLE_STUDENT'),(4,'ROLE_PARENT');
/*!40000 ALTER TABLE `roles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rooms`
--

DROP TABLE IF EXISTS `rooms`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rooms` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `capacity` int NOT NULL,
  `name` varchar(255) NOT NULL,
  `status` enum('AVAILABLE','UNAVAILABLE') DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK1kuqhbfxed2e8t571uo82n545` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rooms`
--

LOCK TABLES `rooms` WRITE;
/*!40000 ALTER TABLE `rooms` DISABLE KEYS */;
INSERT INTO `rooms` VALUES (1,40,'A','AVAILABLE'),(2,50,'B','AVAILABLE'),(3,30,'C','AVAILABLE'),(4,45,'D','AVAILABLE'),(5,40,'A_1','AVAILABLE'),(6,40,'B_1','AVAILABLE'),(7,30,'E','AVAILABLE');
/*!40000 ALTER TABLE `rooms` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `semesters`
--

DROP TABLE IF EXISTS `semesters`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `semesters` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `end_date` date NOT NULL,
  `name` varchar(255) NOT NULL,
  `start_date` date NOT NULL,
  `status` enum('CLOSED','OPEN') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKci1s5s8npb7j044md3s0wdhsh` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `semesters`
--

LOCK TABLES `semesters` WRITE;
/*!40000 ALTER TABLE `semesters` DISABLE KEYS */;
/*!40000 ALTER TABLE `semesters` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `session_chapters`
--

DROP TABLE IF EXISTS `session_chapters`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `session_chapters` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `chapter_id` bigint NOT NULL,
  `session_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKc4p6o1ad0tvt897xyjjyfqijw` (`session_id`,`chapter_id`),
  KEY `FK1asd0v0kemmgcew1hqolrglgy` (`chapter_id`),
  CONSTRAINT `FK1asd0v0kemmgcew1hqolrglgy` FOREIGN KEY (`chapter_id`) REFERENCES `course_chapters` (`id`),
  CONSTRAINT `FK81rtb86fwncojtux5bsgynkn1` FOREIGN KEY (`session_id`) REFERENCES `class_sessions` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `session_chapters`
--

LOCK TABLES `session_chapters` WRITE;
/*!40000 ALTER TABLE `session_chapters` DISABLE KEYS */;
/*!40000 ALTER TABLE `session_chapters` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `session_content_configs`
--

DROP TABLE IF EXISTS `session_content_configs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `session_content_configs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `base_course_id` bigint DEFAULT NULL,
  `chapter_id` bigint DEFAULT NULL,
  `lesson_id` bigint DEFAULT NULL,
  `source_type` varchar(16) NOT NULL,
  `teacher_course_id` bigint DEFAULT NULL,
  `session_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_scc_session` (`session_id`),
  CONSTRAINT `FKsdrssrnogin5etbdw6q1190yo` FOREIGN KEY (`session_id`) REFERENCES `class_sessions` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `session_content_configs`
--

LOCK TABLES `session_content_configs` WRITE;
/*!40000 ALTER TABLE `session_content_configs` DISABLE KEYS */;
/*!40000 ALTER TABLE `session_content_configs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `session_lessons`
--

DROP TABLE IF EXISTS `session_lessons`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `session_lessons` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `lesson_id` bigint NOT NULL,
  `session_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKjlrjtc34wpwyrraq61gvu5act` (`session_id`,`lesson_id`),
  KEY `FK248g9unncaaoohx9jcsebp1ld` (`lesson_id`),
  CONSTRAINT `FK248g9unncaaoohx9jcsebp1ld` FOREIGN KEY (`lesson_id`) REFERENCES `course_lessons` (`id`),
  CONSTRAINT `FKio0vthnqh1q3mi31kngh4w3w0` FOREIGN KEY (`session_id`) REFERENCES `class_sessions` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `session_lessons`
--

LOCK TABLES `session_lessons` WRITE;
/*!40000 ALTER TABLE `session_lessons` DISABLE KEYS */;
/*!40000 ALTER TABLE `session_lessons` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `session_materials`
--

DROP TABLE IF EXISTS `session_materials`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `session_materials` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `description` varchar(500) DEFAULT NULL,
  `file_name` varchar(255) NOT NULL,
  `file_size` bigint DEFAULT NULL,
  `file_type` varchar(100) DEFAULT NULL,
  `file_url` varchar(500) NOT NULL,
  `uploaded_at` datetime(6) NOT NULL,
  `session_id` bigint NOT NULL,
  `uploaded_by` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_session_material_session` (`session_id`),
  KEY `FKfhqc2c2n06p00ai7raa2gojpu` (`uploaded_by`),
  CONSTRAINT `FKfhqc2c2n06p00ai7raa2gojpu` FOREIGN KEY (`uploaded_by`) REFERENCES `users` (`id`),
  CONSTRAINT `FKruxmdouk2y82xprkd6geuodp0` FOREIGN KEY (`session_id`) REFERENCES `class_sessions` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `session_materials`
--

LOCK TABLES `session_materials` WRITE;
/*!40000 ALTER TABLE `session_materials` DISABLE KEYS */;
/*!40000 ALTER TABLE `session_materials` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `students`
--

DROP TABLE IF EXISTS `students`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `students` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `dob` date DEFAULT NULL,
  `grade` varchar(255) DEFAULT NULL,
  `school` varchar(255) DEFAULT NULL,
  `parent_id` bigint DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `avatar_url` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKg4fwvutq09fjdlb4bb0byp7t` (`user_id`),
  KEY `FK7bbpphkk8f0aoav3iiih3mh4e` (`parent_id`),
  CONSTRAINT `FK7bbpphkk8f0aoav3iiih3mh4e` FOREIGN KEY (`parent_id`) REFERENCES `parents` (`id`),
  CONSTRAINT `FKdt1cjx5ve5bdabmuuf3ibrwaq` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `students`
--

LOCK TABLES `students` WRITE;
/*!40000 ALTER TABLE `students` DISABLE KEYS */;
/*!40000 ALTER TABLE `students` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `subjects`
--

DROP TABLE IF EXISTS `subjects`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `subjects` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `status` enum('AVAILABLE','UNAVAILABLE') DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKaodt3utnw0lsov4k9ta88dbpr` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `subjects`
--

LOCK TABLES `subjects` WRITE;
/*!40000 ALTER TABLE `subjects` DISABLE KEYS */;
INSERT INTO `subjects` VALUES (1,'Toán 6','AVAILABLE'),(2,'Toán 7','AVAILABLE'),(3,'Toán 8','AVAILABLE'),(4,'Toán 9','AVAILABLE'),(5,'Ngữ Văn 6','AVAILABLE'),(6,'Ngữ Văn 7','AVAILABLE'),(7,'Ngữ Văn 8','AVAILABLE'),(8,'Ngữ Văn 9','AVAILABLE'),(9,'Tiếng Anh 6','AVAILABLE'),(10,'Tiếng Anh 7','AVAILABLE'),(11,'Tiếng Anh 8','AVAILABLE'),(12,'Tiếng Anh 9','AVAILABLE');
/*!40000 ALTER TABLE `subjects` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `teacher_certificates`
--

DROP TABLE IF EXISTS `teacher_certificates`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teacher_certificates` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `description` text,
  `organization` varchar(255) DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `year` int DEFAULT NULL,
  `teacher_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK32o8s6u2x803ycr4a5hkjwhau` (`teacher_id`),
  CONSTRAINT `FK32o8s6u2x803ycr4a5hkjwhau` FOREIGN KEY (`teacher_id`) REFERENCES `teachers` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `teacher_certificates`
--

LOCK TABLES `teacher_certificates` WRITE;
/*!40000 ALTER TABLE `teacher_certificates` DISABLE KEYS */;
INSERT INTO `teacher_certificates` VALUES (1,'Tập trung vào phương pháp tiếp cận chủ động (Active Learning) và tích hợp công nghệ (GeoGebra) trong giảng dạy.','Viện Khoa học Giáo dục Việt Nam','Chứng chỉ Nghiên cứu Phương pháp Giảng dạy Toán Hiện đại',2024,1),(2,'Chứng nhận năng lực sử dụng tiếng Anh trong môi trường làm việc và nghiên cứu quốc tế.','ETS (Educational Testing Service)','TOEIC 850+',2021,1),(3,'Hoàn thành khóa đào tạo về kỹ năng quản lý lớp học, truyền động lực và làm việc nhóm hiệu quả.','FPT Education','Chứng chỉ Quản lý & Lãnh đạo Đội nhóm (Team Leadership)',2020,1),(4,'Tập huấn chuyên sâu về các dạng bài toán nâng cao, giải tích và số học phục vụ cho việc bồi dưỡng học sinh giỏi.','Trường Đại học Sư phạm Hà Nội','Chứng chỉ Bồi dưỡng Giáo viên Olympic Toán',2018,1),(5,'Nắm vững kỹ năng xử lý dữ liệu và sử dụng SQL, hỗ trợ giảng dạy môn Toán ứng dụng và Xác suất Thống kê.','Coursera (IBM)','Chứng chỉ SQL & Data Analysis Fundamentals',2023,1),(6,'Tập trung vào kỹ năng phân tích tác phẩm tự sự và thơ, ứng dụng các phương pháp dạy học sáng tạo (Project-Based Learning).','Viện Nghiên cứu Sư phạm','Chứng chỉ Phương pháp Giảng dạy Ngữ Văn THCS',2023,2),(7,'Hoàn thành khóa đào tạo về sử dụng các công cụ số hóa để thiết kế bài giảng trực quan và quản lý lớp học trực tuyến hiệu quả.','Trung tâm e360edu PRO','Chứng chỉ Kỹ thuật Ứng dụng Công nghệ trong Giảng dạy (E-Learning)',2024,2),(8,'Nâng cao khả năng hướng dẫn học sinh lập luận chặt chẽ và sử dụng dẫn chứng sắc bén trong các bài văn nghị luận.','Trung tâm Phát triển Kỹ năng (Skill Development Center)','Chứng chỉ Kỹ năng Viết & Phân tích Nghị luận Xã hội',2022,2),(9,'Chứng nhận năng lực sử dụng tiếng Anh ở mức độ thành thạo, đặc biệt trong môi trường học thuật và nghiên cứu.','IDP Education','IELTS Academic 8.0 (Overall)',2023,3),(10,'Chứng chỉ quốc tế về phương pháp giảng dạy tiếng Anh cho người nói các ngôn ngữ khác, tập trung vào kỹ năng thực hành và giao tiếp.','Cambridge Assessment English','TESOL (Teaching English to Speakers of Other Languages)',2021,3),(11,'Tập huấn chuyên sâu về cách thiết kế chương trình học hiệu quả và xây dựng tiêu chí đánh giá năng lực ngôn ngữ toàn diện cho học sinh.','Sở Giáo dục & Đào tạo','Chứng chỉ Phát triển Chương trình và Đánh giá Năng lực',2020,3);
/*!40000 ALTER TABLE `teacher_certificates` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `teacher_course_versions`
--

DROP TABLE IF EXISTS `teacher_course_versions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teacher_course_versions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `base_course_id` bigint NOT NULL,
  `teacher_id` bigint NOT NULL,
  `teacher_course_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKhq7544rd99jtlj1juq37lmg3q` (`base_course_id`,`teacher_course_id`,`teacher_id`),
  KEY `idx_tcv_base_teacher` (`base_course_id`,`teacher_id`),
  KEY `idx_tcv_teacher_course` (`teacher_course_id`,`teacher_id`),
  KEY `FK9yu0md3jxg8ktt4pdmnq3ramv` (`teacher_id`),
  CONSTRAINT `FK9yu0md3jxg8ktt4pdmnq3ramv` FOREIGN KEY (`teacher_id`) REFERENCES `teachers` (`id`),
  CONSTRAINT `FKfx65807iddl3ai14esmg7pygp` FOREIGN KEY (`base_course_id`) REFERENCES `courses` (`id`),
  CONSTRAINT `FKrb1girej86gfc07ggl6miiqw6` FOREIGN KEY (`teacher_course_id`) REFERENCES `courses` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `teacher_course_versions`
--

LOCK TABLES `teacher_course_versions` WRITE;
/*!40000 ALTER TABLE `teacher_course_versions` DISABLE KEYS */;
/*!40000 ALTER TABLE `teacher_course_versions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `teacher_education`
--

DROP TABLE IF EXISTS `teacher_education`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teacher_education` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `degree` varchar(255) NOT NULL,
  `description` text,
  `school` varchar(255) DEFAULT NULL,
  `year` int DEFAULT NULL,
  `teacher_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKgljj7h83vlueadgygup1uxii1` (`teacher_id`),
  CONSTRAINT `FKgljj7h83vlueadgygup1uxii1` FOREIGN KEY (`teacher_id`) REFERENCES `teachers` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `teacher_education`
--

LOCK TABLES `teacher_education` WRITE;
/*!40000 ALTER TABLE `teacher_education` DISABLE KEYS */;
INSERT INTO `teacher_education` VALUES (1,'Thạc sĩ (Master)','Chuyên ngành: Toán Ứng dụng. Luận văn Thạc sĩ về mô hình hóa toán học trong kinh tế.','Đại học Bách Khoa Hà Nội',2015,1),(2,'Cử nhân (Bachelor)','Chuyên ngành: Sư phạm Toán. Tốt nghiệp loại Giỏi, tham gia nghiên cứu khoa học sinh viên.','Đại học Sư phạm Hà Nội',2013,1),(3,'Chứng chỉ Nghiên cứu Sau Đại học','Tham gia khóa học chuyên đề nâng cao về Đại số và Hình học không gian.','Viện Toán học (VAST)',2017,1),(4,'Chương trình trao đổi ngắn hạn','Tham gia chương trình giao lưu học thuật 3 tháng về Phương pháp nghiên cứu Toán học.','University of New South Wales (Úc)',2014,1),(5,'Chuyên đề Phát triển Giáo trình','Hoàn thành khóa bồi dưỡng nội bộ về xây dựng và thiết kế giáo trình theo chuẩn Quốc tế.','DH FPT Đà Nẵng',2022,1),(6,'Cử nhân (Bachelor)','Chuyên ngành: Ngữ văn. Tốt nghiệp loại Khá, khóa luận về Văn học Dân gian.','Đại học Sư phạm Hà Nội',2018,2),(7,'Khóa học Bồi dưỡng Chuyên sâu Văn học Hiện đại','Tham gia khóa học ngắn hạn tập trung vào các tác giả tiêu biểu của Văn học Việt Nam sau năm 1975.','Đại học Khoa học Xã hội và Nhân văn',2021,2),(8,'Chứng chỉ Nghiên cứu Văn hóa Dân gian','Hoàn thành chương trình tìm hiểu chuyên sâu về Truyền thuyết, Cổ tích và Thần thoại, phục vụ giảng dạy lớp 6.','Hội Văn học Nghệ thuật Dân gian Việt Nam',2023,2),(9,'Thạc sĩ (Master)','Chuyên ngành: Ngôn ngữ Anh. Luận văn nghiên cứu về các yếu tố ảnh hưởng đến khả năng Giao tiếp Học thuật (Academic Communication).','Đại học Ngoại ngữ - ĐHQG Hà Nội',2018,3),(10,'Cử nhân (Bachelor)','Chuyên ngành: Sư phạm Tiếng Anh. Tốt nghiệp loại Giỏi, tham gia chương trình thực tập tại nước ngoài.','Đại học Sư phạm Ngoại ngữ',2016,3),(11,'Chương trình trao đổi ngắn hạn','Tham gia khóa học 6 tháng về Ngữ pháp Tiếng Anh chuyên sâu và Văn hóa Mỹ.','University of Oregon (Hoa Kỳ)',2015,3);
/*!40000 ALTER TABLE `teacher_education` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `teacher_experience`
--

DROP TABLE IF EXISTS `teacher_experience`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teacher_experience` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `company` varchar(255) DEFAULT NULL,
  `description` text,
  `end_year` int DEFAULT NULL,
  `position` varchar(255) DEFAULT NULL,
  `start_year` int DEFAULT NULL,
  `teacher_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKrne9sxuo028krmvwcsv6gfcp0` (`teacher_id`),
  CONSTRAINT `FKrne9sxuo028krmvwcsv6gfcp0` FOREIGN KEY (`teacher_id`) REFERENCES `teachers` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `teacher_experience`
--

LOCK TABLES `teacher_experience` WRITE;
/*!40000 ALTER TABLE `teacher_experience` DISABLE KEYS */;
INSERT INTO `teacher_experience` VALUES (1,'Đại học FPT Đà Nẵng','Giảng dạy các môn Toán Cao cấp, Toán rời rạc. Chủ nhiệm CLB Toán Học và Hỗ trợ nghiên cứu khoa học sinh viên.',2025,'Giảng viên Chính thức',2015,1),(2,'Trung tâm Luyện thi ABC','Phụ trách lớp luyện thi Đại học khối A/A1, giúp hơn 90% học sinh đạt trên 8.0 điểm môn Toán.',2023,'Chuyên viên Đào tạo (Part-time)',2018,1),(3,'Sở GD&ĐT Đà Nẵng','Phụ trách lớp chuyên đề bồi dưỡng học sinh giỏi Toán cấp Tỉnh và cấp Quốc gia.',2021,'Chủ nhiệm Lớp Bồi dưỡng HSG',2019,1),(4,'Đại học Bách Khoa Hà Nội','Hỗ trợ giảng viên chính, chấm bài và tham gia nghiên cứu về thuật toán tối ưu.',2015,'Trợ giảng/Nghiên cứu viên',2013,1),(5,'Công ty Sách Giáo dục XYZ','Tham gia nhóm biên soạn tài liệu tham khảo và sách bài tập nâng cao môn Toán THCS/THPT.',2025,'Biên soạn Chương trình',2024,1),(6,'Trung tâm e360edu ','Phụ trách giảng dạy các khối lớp 6, 9. Chịu trách nhiệm bồi dưỡng học sinh có nhu cầu thi chuyên, thi vào 10.',2025,'Giáo viên Ngữ văn Chính thức',2020,2),(7,'NXB Giáo dục','Tham gia biên tập và xây dựng ngân hàng đề thi, bài tập nâng cao môn Ngữ văn cho học sinh THCS.',2024,'Cộng tác viên Biên soạn Tài liệu',2022,2),(8,'Trường THCS Lương Thế Vinh','Hoàn thành thời gian thực tập sư phạm, đạt kết quả xuất sắc trong công tác chủ nhiệm và giảng dạy.',2018,'Giáo viên THCS (Thực tập)',2017,2),(9,'THPT Hoàng Hoa Thám','Phụ trách giảng dạy chính khối 11, 12 và đội tuyển học sinh giỏi. Chịu trách nhiệm về điểm thi tốt nghiệp THPT Quốc gia môn Tiếng Anh.',2025,'Giáo viên Tiếng Anh',2018,3),(10,'Trung tâm Anh ngữ XYZ','Giảng dạy các lớp luyện thi chứng chỉ quốc tế, giúp học viên đạt mục tiêu điểm số từ 6.5 IELTS trở lên.',2023,'Giáo viên Luyện thi IELTS/TOEIC',2019,3),(11,'Dự án Học thuật/Kinh doanh','Dịch thuật các tài liệu chuyên ngành giáo dục và tài chính, duy trì khả năng sử dụng ngôn ngữ linh hoạt và chính xác.',2018,'Biên dịch viên Tự do (Freelance Translator)',2016,3);
/*!40000 ALTER TABLE `teacher_experience` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `teacher_subjects`
--

DROP TABLE IF EXISTS `teacher_subjects`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teacher_subjects` (
  `teacher_id` bigint NOT NULL,
  `subject_id` bigint NOT NULL,
  PRIMARY KEY (`teacher_id`,`subject_id`),
  KEY `FKdweqkwxroox2u7pbmksehx04i` (`subject_id`),
  CONSTRAINT `FK6dcl3ihufp4v0j1fuxlw4ksoj` FOREIGN KEY (`teacher_id`) REFERENCES `teachers` (`id`),
  CONSTRAINT `FKdweqkwxroox2u7pbmksehx04i` FOREIGN KEY (`subject_id`) REFERENCES `subjects` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `teacher_subjects`
--

LOCK TABLES `teacher_subjects` WRITE;
/*!40000 ALTER TABLE `teacher_subjects` DISABLE KEYS */;
INSERT INTO `teacher_subjects` VALUES (1,1),(2,2),(6,2),(3,3),(4,4),(5,5),(6,7);
/*!40000 ALTER TABLE `teacher_subjects` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `teachers`
--

DROP TABLE IF EXISTS `teachers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teachers` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `achievements` text,
  `avatar_url` longtext,
  `bio` text,
  `degree` varchar(50) DEFAULT NULL,
  `facebook_url` varchar(500) DEFAULT NULL,
  `linkedin_url` varchar(500) DEFAULT NULL,
  `note` varchar(1000) DEFAULT NULL,
  `rating` double DEFAULT NULL,
  `specialization` varchar(255) DEFAULT NULL,
  `workplace` varchar(255) DEFAULT NULL,
  `years_of_experience` int DEFAULT NULL,
  `subject_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKcd1k6xwg9jqtiwx9ybnxpmoh9` (`user_id`),
  KEY `FKsahkj7ew9hfs6byrpl75br5lx` (`subject_id`),
  CONSTRAINT `FKb8dct7w2j1vl1r2bpstw5isc0` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKsahkj7ew9hfs6byrpl75br5lx` FOREIGN KEY (`subject_id`) REFERENCES `subjects` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `teachers`
--

LOCK TABLES `teachers` WRITE;
/*!40000 ALTER TABLE `teachers` DISABLE KEYS */;
INSERT INTO `teachers` VALUES (1,'Giảng viên xuất sắc năm 2023 tại ĐH FPT. Có học sinh đạt giải Nhất/Nhì Kỳ thi HSG Toán cấp Tỉnh. 95% học sinh luyện thi đỗ vào các trường top đầu (Bách Khoa, Ngoại Thương, Kinh tế).','https://sf-static.upanhlaylink.com/img/image_2025120102c4ef99ec67c15ccf21f0b01cac3cc6.jpg','Thầy Nguyễn Ngọc Hùng là Thạc sĩ Toán Ứng dụng với hơn 10 năm kinh nghiệm giảng dạy và nghiên cứu tại Trường Đại học FPT Đà Nẵng. Thầy chuyên sâu trong việc xây dựng nền tảng Toán học vững chắc cho học sinh THPT và luyện thi Đại học khối A/A1. Với triết lý \"Toán học là công cụ tư duy\", thầy Hùng luôn giúp học sinh không chỉ giải được bài toán mà còn hiểu rõ bản chất vấn đề.','Thạc sĩ','https://www.facebook.com/ighoorbeos','https://www.facebook.com/ighoorbeos','Đã tham gia biên soạn tài liệu ôn thi chuyên đề \"Phân tích Số học và Xác suất\" cho học sinh lớp 6.',0,'Toán Cao cấp, Phương pháp Giảng dạy Toán học trực quan, Bồi dưỡng học sinh Giỏi cấp Tỉnh.','DH FPT ĐÀ NẮNG',10,1,3),(2,'Giáo viên được học sinh yêu thích nhất tại Trung tâm e360edu PRO năm 2024. 98% học sinh đạt điểm Giỏi môn Văn trong các kỳ thi học kỳ. Có học sinh đạt Giải Ba cuộc thi Viết Văn cấp Quận.','https://sf-static.upanhlaylink.com/img/image_202512014fa21775e03275b7cc8f1dfb3160b11b.jpg','Cô Dương Uyển Nhi là Cử nhân Ngữ văn với 5 năm kinh nghiệm giảng dạy, chuyên sâu trong việc truyền cảm hứng đọc hiểu và viết văn cho học sinh THCS. Cô Nhi nổi bật với phong cách dạy học nhẹ nhàng, sâu lắng, giúp học sinh không còn sợ môn Văn mà thay vào đó là biết cách cảm thụ, phân tích tác phẩm và tự tin thể hiện cảm xúc qua từng câu chữ.','Cử nhân','https://www.facebook.com/ighoorbeos','https://www.facebook.com/ighoorbeos','Là thành viên của nhóm biên soạn tài liệu \"Tuyển tập Nghị luận 9+\".',0,'Phân tích Truyện Tự sự và Ký, Kỹ năng Viết Nghị luận Xã hội, Phương pháp Giảng dạy theo dự án (Project-Based Learning).','Trung tâm e360edu PRO',5,2,4),(3,'Giáo viên có thành tích bồi dưỡng học sinh giỏi Tiếng Anh cấp Quận/Tỉnh. Có 90% học sinh đạt điểm Giỏi môn Tiếng Anh trong kỳ thi THPT Quốc gia. Giảng viên được học sinh đánh giá cao về phương pháp giảng dạy năng động.','https://sf-static.upanhlaylink.com/img/image_202512011e50286e181ba26fc94fb69e49367d91.jpg','Thầy Khiếu Anh Tuấn là Thạc sĩ Ngôn ngữ Anh với 7 năm kinh nghiệm giảng dạy tại trường THPT Hoàng Hoa Thám. Thầy chuyên sâu trong việc phát triển kỹ năng Giao tiếp và Ngữ pháp Học thuật cho học sinh THCS và THPT. Thầy Tuấn sử dụng phương pháp giảng dạy hiện đại, nhấn mạnh vào việc ứng dụng ngôn ngữ trong thực tế để giúp học sinh tiến bộ nhanh chóng và yêu thích môn học.','Tiến sĩ','https://www.facebook.com/ighoorbeos','https://www.facebook.com/ighoorbeos','Là tác giả của chuyên đề \"Phát âm chuẩn và Ngữ điệu tự nhiên\" cho học sinh cấp 2.',0,'Luyện thi IELTS/TOEIC, Ngữ pháp chuyên sâu, Kỹ năng Giao tiếp ứng dụng (Communicative Method).','THPT Hoàng Hoa Thám',7,3,5),(4,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,NULL,NULL,0,4,10),(5,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,NULL,NULL,0,5,11),(6,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,NULL,NULL,0,2,12);
/*!40000 ALTER TABLE `teachers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `time_slots`
--

DROP TABLE IF EXISTS `time_slots`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `time_slots` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `end_time` time(6) NOT NULL,
  `start_time` time(6) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `time_slots`
--

LOCK TABLES `time_slots` WRITE;
/*!40000 ALTER TABLE `time_slots` DISABLE KEYS */;
INSERT INTO `time_slots` VALUES (1,'18:00:00.000000','16:00:00.000000'),(2,'20:00:00.000000','18:00:00.000000'),(3,'22:00:00.000000','20:00:00.000000');
/*!40000 ALTER TABLE `time_slots` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_roles`
--

DROP TABLE IF EXISTS `user_roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_roles` (
  `user_id` bigint NOT NULL,
  `role_id` int NOT NULL,
  PRIMARY KEY (`user_id`,`role_id`),
  KEY `FKh8ciramu9cc9q3qcqiv4ue8a6` (`role_id`),
  CONSTRAINT `FKh8ciramu9cc9q3qcqiv4ue8a6` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`),
  CONSTRAINT `FKhfh9dx7w3ubf1co1vdev94g3f` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_roles`
--

LOCK TABLES `user_roles` WRITE;
/*!40000 ALTER TABLE `user_roles` DISABLE KEYS */;
INSERT INTO `user_roles` VALUES (2,1),(3,2),(4,2),(5,2),(10,2),(11,2),(12,2),(7,3),(9,3),(1,4),(6,4),(8,4);
/*!40000 ALTER TABLE `user_roles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `email` varchar(50) NOT NULL,
  `full_name` varchar(100) NOT NULL,
  `password` varchar(120) NOT NULL,
  `phone_number` varchar(15) NOT NULL,
  `username` varchar(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKr43af9ap4edm43mmtq01oddj6` (`username`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,_binary '','mikiigamera3@gmail.com','Nguyễn Thị Y','$2a$10$d7In7dQ4iJorpdwpuJ/q3u8qWG5Zgcu1QyD2X2doNcxLYxT581hWO','0963398715','ynt'),(2,_binary '','mikiigamera33@gmail.com','admin','$2a$10$1qPVDwMzPBK48EZQEwHyvuyX64NB.4r7KTfDAc7Yu/9JoCREdiK6i','0963398715','admin'),(3,_binary '','hungnnhe173218@fpt.edu.vn','Nguyễn Ngọc Hùng','$2a$10$dIt0xOMUTI/nATA3bePTTuloruWHkkdjAYDO.JJ/RjDYAufepN6.e','0963398714','hùngnn'),(4,_binary '','ighoorbeos@gmail.com','Dương Uyển Nhi','$2a$10$AMlVR4zQ2WSLtnznfzWCJ.NNpf6f1t7VfuU3o1oQ/4wWTNOILsUO2','0123456789','nhidu'),(5,_binary '','lopa3k54lg1@gmail.com','Khiếu Anh Tuấn','$2a$10$TYi1q8XMtuZcEV5Qo8gJBuamPK4C1EAA6W6FAZPrXp3r.mtA/NtuS','0123456788','tuấnka'),(6,_binary '','mikiigamera333@gmail.com','Nguyễn Thị Y','$2a$10$2163ilFhhYJXeU4r7.I49.1.DF1hlVwkyPtOdjZygipIR5EnFQ1/u','0963398715','ynt1'),(7,_binary '','hakhanhlinh@gmail.com','Hà Khánh Linh','$2a$10$dm2lt/4/LUZ/QJ8KZNr5teG3XI882en0sFHoNfJE1dz/gy/vqtBEe','0963398723','HaKhanhLinh'),(8,_binary '','mikiigamera32@gmail.com','Parent1','$2a$10$SyUskUWm/i3teVlsZU2/yud2.nCD/fHIfbiR7GUuw7QsbxcanGfNu','0963398715','parent1'),(9,_binary '','student1@gmail.com','student1','$2a$10$1bvl1sg/SXcR81EfQ8.zuuo2SMFULlf0jMsvH/gVxO9MEZ.DkSDw.','0963398723','student1'),(10,_binary '','vudon2003@gmail.com','Vũ Quý Đôn','$2a$10$ZloZSzrNqyOpTjuyODVzbOO2zKT8whtzogHufqr7dF8uAD7rKzpwa','0123456788','đônvq'),(11,_binary '','tuanthaifpt@gmail.com','Nguyễn Nam Anh Tuấn','$2a$10$FLBUXjpD289Ll9qMkeaqmOrpL72v.VMMg6oPazoRexSnRkm8p3bNO','0123456786','tuấnnna'),(12,_binary '','tuan.makingjob@gmail.com','Đặng Thị Thu Giang','$2a$10$NuBPMmNbTC.eJLSLAtmZnu4lHPSA1MP98yzI8pu5u3A1N0Q3T75g.','0983504135','giangđtt');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-12-05  9:24:29
