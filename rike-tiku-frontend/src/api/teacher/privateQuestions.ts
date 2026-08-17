import http from "../http";
import type { Detail, Save } from "../admin/questions";
export interface PrivateQuestion {
  id: number;
  teachingAssignmentId: number;
  className: string;
  subjectName: string;
  questionType: string;
  stem: string;
  status: string;
}
export const fetchPrivateQuestions = () =>
  http.get<PrivateQuestion[]>("/teacher/private-questions").then((r) => r.data);
export const fetchPrivateQuestion = (id: number) =>
  http.get<Detail>(`/teacher/private-questions/${id}`).then((r) => r.data);
export const createPrivateQuestion = (scopeId: number, body: Save) =>
  http
    .post<PrivateQuestion>("/teacher/private-questions", body, {
      params: { teachingAssignmentId: scopeId },
    })
    .then((r) => r.data);
export const updatePrivateQuestion = (id: number, body: Save) =>
  http
    .put<PrivateQuestion>(`/teacher/private-questions/${id}`, body)
    .then((r) => r.data);
export const publishPrivateQuestion = (id: number) =>
  http
    .post<PrivateQuestion>(`/teacher/private-questions/${id}/publish`)
    .then((r) => r.data);
export const submitPrivateQuestionToAdmin = (id: number) =>
  http
    .post<PrivateQuestion>(`/teacher/private-questions/${id}/submit-admin`)
    .then((r) => r.data);
export const disablePrivateQuestion = (id: number) =>
  http
    .post<PrivateQuestion>(`/teacher/private-questions/${id}/disable`)
    .then((r) => r.data);
export const deletePrivateQuestion = (id: number) =>
  http.delete(`/teacher/private-questions/${id}`);
