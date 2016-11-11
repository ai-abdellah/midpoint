/*
 * Copyright (c) 2010-2016 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.web.page.admin.users;

import com.evolveum.midpoint.gui.api.ComponentConstants;
import com.evolveum.midpoint.gui.api.component.ObjectBrowserPanel;
import com.evolveum.midpoint.gui.api.component.TypedAssignablePanel;
import com.evolveum.midpoint.gui.api.component.tabs.CountablePanelTab;
import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.FocusTabVisibleBehavior;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.query.InOidFilter;
import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDto;
import com.evolveum.midpoint.web.component.assignment.AssignmentTablePanel;
import com.evolveum.midpoint.web.component.assignment.DelegationEditorPanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.FocusSummaryPanel;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectMainPanel;
import com.evolveum.midpoint.web.component.objectdetails.FocusMainPanel;
import com.evolveum.midpoint.web.page.admin.PageAdminFocus;
import com.evolveum.midpoint.web.page.admin.users.component.UserSummaryPanel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author lazyman
 * @author semancik
 */
@PageDescriptor(url = "/admin/user", encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = PageAdminUsers.AUTH_USERS_ALL,
                label = PageAdminUsers.AUTH_USERS_ALL_LABEL,
                description = PageAdminUsers.AUTH_USERS_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_USER_URL,
                label = "PageUser.auth.user.label",
                description = "PageUser.auth.user.description")})
public class PageUser extends PageAdminFocus<UserType> {

    private static final String DOT_CLASS = PageUser.class.getName() + ".";
    private static final String OPERATION_LOAD_DELEGATED_TO_ME_ASSIGNMENTS = DOT_CLASS + "loadDelegatedToMeAssignments";
    private static final String OPERATION_LOAD_ASSIGNMENT_PEVIEW_DTO_LIST = DOT_CLASS + "loadAssignmentPreviewDtoList";

    private static final String ID_TASK_TABLE = "taskTable";
    private static final String ID_TASKS = "tasks";
    private LoadableModel<List<AssignmentEditorDto>> delegationsModel;
    private LoadableModel<List<AssignmentEditorDto>> delegatedToMeModel;

    private static final Trace LOGGER = TraceManager.getTrace(PageUser.class);

    public PageUser() {
        initialize(null);
    }

    public PageUser(PageParameters parameters) {
        getPageParameters().overwriteWith(parameters);
        initialize(null);
    }

    public PageUser(final PrismObject<UserType> userToEdit) {
        initialize(userToEdit);
    }

    @Override
    protected void initializeModel(final PrismObject<UserType> objectToEdit) {
        super.initializeModel(objectToEdit);
        delegationsModel = new LoadableModel<List<AssignmentEditorDto>>(false) {
            @Override
            protected List<AssignmentEditorDto> load() {
                return loadMyDelegations();
            }
        };
        delegatedToMeModel= new LoadableModel<List<AssignmentEditorDto>>(false) {
            @Override
            protected List<AssignmentEditorDto> load() {
                return loadDelegatedToMeAssignments();
            }
        };
    }

    @Override
    protected FocusSummaryPanel<UserType> createSummaryPanel() {
    	return new UserSummaryPanel(ID_SUMMARY_PANEL, getObjectModel());
    }

    protected void cancelPerformed(AjaxRequestTarget target) {
        // uncoment later -> check for changes to not allow leave the page when
        // some changes were made
        // try{
        // if (userModel.getObject().getOldDelta() != null &&
        // !userModel.getObject().getOldDelta().isEmpty() ||
        // userModel.getObject().getFocusPrimaryDelta() != null &&
        // !userModel.getObject().getFocusPrimaryDelta().isEmpty()){
        // showModalWindow(MODAL_ID_CONFIRM_CANCEL, target);
        // } else{
        redirectBack();

        // }
        // }catch(Exception ex){
        // LoggingUtils.logUnexpectedException(LOGGER, "Could not return to user list",
        // ex);
        // }
    }
    

	@Override
	protected UserType createNewObject() {
		return new UserType();
	}
	
	@Override
	protected Class getRestartResponsePage() {
		return PageUsers.class;
	}
	
	@Override
    public Class getCompileTimeClass() {
		return UserType.class;
	}

	@Override
	protected AbstractObjectMainPanel<UserType> createMainPanel(String id) {
        return new FocusMainPanel<UserType>(id, getObjectModel(), getAssignmentsModel(), getProjectionModel(), this) {
            @Override
            protected void addSpecificTabs(final PageAdminObjectDetails<UserType> parentPage, List<ITab> tabs) {
                FocusTabVisibleBehavior authorization = new FocusTabVisibleBehavior(unwrapModel(), ComponentConstants.UI_FOCUS_TAB_OBJECT_HISTORY_URL);
                tabs.add(
                        new PanelTab(parentPage.createStringResource("pageAdminFocus.objectHistory"), authorization) {

                            private static final long serialVersionUID = 1L;

                            @Override
                            public WebMarkupContainer createPanel(String panelId) {
                                return createObjectHistoryTabPanel(panelId, parentPage);
                            }
                        });
                authorization = new FocusTabVisibleBehavior(unwrapModel(),
                        ComponentConstants.UI_FOCUS_TAB_DELEGATIONS_URL);
                tabs.add(new CountablePanelTab(parentPage.createStringResource("FocusType.delegations"), authorization)
                {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public WebMarkupContainer createPanel(String panelId) {
                        return new AssignmentTablePanel<UserType>(panelId, parentPage.createStringResource("FocusType.delegations"),
                                delegationsModel) {
                            private static final long serialVersionUID = 1L;

                            @Override
                            public void populateItem(ListItem<AssignmentEditorDto> item) {
                                DelegationEditorPanel editor = new DelegationEditorPanel(ID_ROW, item.getModel(),
                                        false, PageUser.this);
                                item.add(editor);
                            }

                            @Override
                            public String getExcludeOid() {
                                return getObject().getOid();
                            }

                            @Override
                            protected List<InlineMenuItem> createAssignmentMenu() {
                                List<InlineMenuItem> items = new ArrayList<>();

                                InlineMenuItem item;
                                if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ASSIGN_ACTION_URL)) {
                                    item = new InlineMenuItem(createStringResource("AssignmentTablePanel.menu.addDelegation"),
                                            new InlineMenuItemAction() {
                                                private static final long serialVersionUID = 1L;

                                                @Override
                                                public void onClick(AjaxRequestTarget target) {
                                                    List<QName> supportedTypes = new ArrayList<>();
                                                    supportedTypes.add(UserType.COMPLEX_TYPE);
                                                    ObjectFilter filter = InOidFilter.createInOid(getObjectWrapper().getOid());
                                                    ObjectFilter notFilter = NotFilter.createNot(filter);
                                                    ObjectBrowserPanel<UserType> panel = new ObjectBrowserPanel<UserType>(
                                                            getMainPopupBodyId(), UserType.class,
                                                            supportedTypes, false, PageUser.this, notFilter) {
                                                        private static final long serialVersionUID = 1L;

                                                        @Override
                                                        protected void onSelectPerformed(AjaxRequestTarget target, UserType user) {
                                                            hideMainPopup(target);
                                                            List<ObjectType> newAssignmentsList = new ArrayList<ObjectType>();
                                                            newAssignmentsList.add(user);
                                                            addSelectedAssignablePerformed(target, newAssignmentsList, getPageBase().getMainPopup().getId());
                                                        }

                                                    };
                                                    panel.setOutputMarkupId(true);
                                                    showMainPopup(panel, target);

                                                }
                                            });
                                    items.add(item);
                                }
                                if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_UNASSIGN_ACTION_URL)) {
                                    item = new InlineMenuItem(createStringResource("AssignmentTablePanel.menu.deleteDelegation"),
                                            new InlineMenuItemAction() {
                                                private static final long serialVersionUID = 1L;

                                                @Override
                                                public void onClick(AjaxRequestTarget target) {
//                                            deleteAssignmentPerformed(target);
                                                }
                                            });
                                    items.add(item);
                                }

                                return items;
                            }
                        };
                    }

                    @Override
                    public String getCount() {
                        return Integer.toString(delegationsModel.getObject() == null ? 0 : delegationsModel.getObject().size());
                    }
                });


            }

        };
    }

    private List<AssignmentEditorDto> loadMyDelegations() {
        List<AssignmentEditorDto> list = new ArrayList<AssignmentEditorDto>();

        ObjectWrapper<UserType> focusWrapper = getObjectModel().getObject();
        PrismObject<UserType> focus = focusWrapper.getObject();
        List<AssignmentType> assignments = focus.asObjectable().getAssignment();
        for (AssignmentType assignment : assignments) {
            if (assignment.getTargetRef() != null &&
                    UserType.COMPLEX_TYPE.equals(assignment.getTargetRef().getType())) {
                list.add(new AssignmentEditorDto(UserDtoStatus.MODIFY, assignment, this));
            }
        }

        Collections.sort(list);

        return list;
    }

    private List<AssignmentEditorDto> loadDelegatedToMeAssignments() {
        OperationResult result = new OperationResult(OPERATION_LOAD_DELEGATED_TO_ME_ASSIGNMENTS);
        List<AssignmentEditorDto> list = new ArrayList<>();
        try{

            Task task = createSimpleTask(OPERATION_LOAD_DELEGATED_TO_ME_ASSIGNMENTS);

            PrismReferenceValue referenceValue = new PrismReferenceValue(getObjectWrapper().getOid(),
                    UserType.COMPLEX_TYPE);

            ObjectFilter refFilter = QueryBuilder.queryFor(UserType.class, getPrismContext())
                    .item(UserType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF).ref(referenceValue)
                    .buildFilter();

            ObjectQuery query = new ObjectQuery();
            query.setFilter(refFilter);

            List<PrismObject<UserType>> usersList = getModelService().searchObjects(UserType.class, query, null, task, result);
            if (usersList != null && usersList.size() > 0){
                for (PrismObject<UserType> user : usersList) {
                    List<AssignmentType> assignments = user.asObjectable().getAssignment();
                    for (AssignmentType assignment : assignments) {
                        if (assignment.getTargetRef() != null &&
                                StringUtils.isNotEmpty(assignment.getTargetRef().getOid()) &&
                                assignment.getTargetRef().getOid().equals(getObjectWrapper().getOid())) {
                            list.add(new AssignmentEditorDto(UserDtoStatus.MODIFY, assignment, this));
                        }
                    }
                }
            }

        } catch (Exception ex){
            result.recomputeStatus();
            showResult(result);
        }


        Collections.sort(list);

        return list;
    }
}
