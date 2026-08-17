import type { AxiosRequestConfig, AxiosResponse } from "axios";
import { afterEach, describe, expect, it, vi } from "vitest";
import http from "../http";
import {
  changeTeachingAssignmentStatus,
  createTeacher,
  createTeachingAssignment,
  fetchTeachers,
  grantTeacherAdmin,
  resetTeacherPassword,
  resetTeacherPasswords,
  revokeTeacherAdmin,
  updateTeacher,
} from "./teachers";

const adapter = vi.fn((config: AxiosRequestConfig): Promise<AxiosResponse> =>
  Promise.resolve({
    data: { records: [] },
    status: 200,
    statusText: "OK",
    headers: {},
    config,
  }),
);
http.defaults.adapter = adapter;
afterEach(() => adapter.mockClear());

describe("教师管理 API", () => {
  it("教师列表提交分页和筛选参数", async () => {
    await fetchTeachers({
      page: 2,
      size: 20,
      employeeNumber: "T01",
      name: "张",
      username: "teacher",
      accountStatus: "ENABLED",
      profileStatus: "ACTIVE",
    });
    expect(adapter).toHaveBeenCalledWith(
      expect.objectContaining({
        method: "get",
        url: "/admin/teachers",
        params: expect.objectContaining({ page: 2, employeeNumber: "T01" }),
      }),
    );
  });
  it("创建教师只提交固定教师字段", async () => {
    await createTeacher({
      employeeNumber: "T01",
      name: "张老师",
      username: "teacher01",
      accountStatus: "ENABLED",
    });
    expect(adapter).toHaveBeenCalledWith(
      expect.objectContaining({
        method: "post",
        url: "/admin/teachers",
        data: expect.not.stringContaining("roles"),
      }),
    );
  });
  it("修改教师不提交工号、用户名或角色", async () => {
    await updateTeacher(5, {
      name: "张老师",
      accountStatus: "DISABLED",
      profileStatus: "ACTIVE",
    });
    expect(adapter).toHaveBeenCalledWith(
      expect.objectContaining({
        method: "put",
        url: "/admin/teachers/5",
        data: expect.not.stringContaining("employeeNumber"),
      }),
    );
  });
  it("任课关系提交真实班级和科目ID并独立变更状态", async () => {
    await createTeachingAssignment(5, {
      classId: 7,
      subjectId: 1,
      primary: true,
      startTime: "2026-08-05T08:00:00.000Z",
    });
    await changeTeachingAssignmentStatus(9, "ENDED");
    expect(adapter).toHaveBeenNthCalledWith(
      1,
      expect.objectContaining({
        method: "post",
        url: "/admin/teachers/5/teaching-assignments",
        data: expect.stringContaining("classId"),
      }),
    );
    expect(adapter).toHaveBeenNthCalledWith(
      2,
      expect.objectContaining({
        method: "patch",
        url: "/admin/teaching-assignments/9/status",
        data: JSON.stringify({ status: "ENDED" }),
      }),
    );
  });
  it("管理员密码重置使用专用一次性响应接口", async () => {
    await resetTeacherPassword(5);
    expect(adapter).toHaveBeenCalledWith(
      expect.objectContaining({
        method: "post",
        url: "/admin/teachers/5/reset-password",
      }),
    );
  });
  it("批量恢复只提交所选教师业务 ID", async () => {
    await resetTeacherPasswords([5, 6]);
    expect(adapter).toHaveBeenCalledWith(
      expect.objectContaining({
        method: "post",
        url: "/admin/teachers/reset-passwords",
        data: JSON.stringify({ ids: [5, 6] }),
      }),
    );
  });
  it("管理员角色授权与撤销使用同一教师角色接口", async () => {
    await grantTeacherAdmin(5);
    await revokeTeacherAdmin(5);
    expect(adapter).toHaveBeenNthCalledWith(
      1,
      expect.objectContaining({
        method: "post",
        url: "/admin/teachers/5/admin-role",
      }),
    );
    expect(adapter).toHaveBeenNthCalledWith(
      2,
      expect.objectContaining({
        method: "delete",
        url: "/admin/teachers/5/admin-role",
      }),
    );
  });
});
